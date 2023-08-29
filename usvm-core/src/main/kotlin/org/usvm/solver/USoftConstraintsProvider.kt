package org.usvm.solver

import io.ksmt.expr.KApp
import io.ksmt.expr.KBvSignedLessOrEqualExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.sort.KArray2Sort
import io.ksmt.sort.KArray3Sort
import io.ksmt.sort.KArrayNSort
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBv1Sort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KFp32Sort
import io.ksmt.sort.KFp64Sort
import io.ksmt.sort.KFpRoundingModeSort
import io.ksmt.sort.KFpSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KRealSort
import io.ksmt.sort.KSort
import io.ksmt.sort.KSortVisitor
import io.ksmt.sort.KUninterpretedSort
import io.ksmt.utils.asExpr
import org.usvm.UAddressSort
import org.usvm.collection.array.UAllocatedArrayReading
import org.usvm.collection.map.primitive.UAllocatedMapReading
import org.usvm.UBoolExpr
import org.usvm.UBvSort
import org.usvm.UCollectionReading
import org.usvm.UConcreteHeapRef
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.UIndexedMethodReturnValue
import org.usvm.collection.array.length.UInputArrayLengthReading
import org.usvm.collection.array.UInputArrayReading
import org.usvm.collection.field.UInputFieldReading
import org.usvm.collection.map.length.UInputMapLengthReading
import org.usvm.collection.map.primitive.UInputMapReading
import org.usvm.UIsSubtypeExpr
import org.usvm.UIsSupertypeExpr
import org.usvm.UMockSymbol
import org.usvm.UNullRef
import org.usvm.URegisterReading
import org.usvm.USizeExpr
import org.usvm.USort
import org.usvm.USymbol
import org.usvm.UTransformer
import org.usvm.collection.map.ref.UAllocatedRefMapWithInputKeysReading
import org.usvm.collection.map.ref.UInputRefMapWithAllocatedKeysReading
import org.usvm.collection.map.ref.UInputRefMapWithInputKeysReading
import org.usvm.uctx
import org.usvm.util.Region

class USoftConstraintsProvider<Type>(override val ctx: UContext) : UTransformer<Type> {
    // We have a list here since sometimes we want to add several soft constraints
    // to make it possible to drop only a part of them, not the whole soft constraint
    private val caches = hashMapOf<UExpr<*>, Set<UBoolExpr>>()
    private val sortPreferredValuesProvider = SortPreferredValuesProvider()

    fun provide(initialExpr: UExpr<*>): Set<UBoolExpr> =
        caches.getOrElse(initialExpr) {
            apply(initialExpr)
            caches.getOrPut(initialExpr, ::emptySet)
        }

    // region The most common methods

    override fun <T : KSort> transformExpr(expr: KExpr<T>): KExpr<T> = computeSideEffect(expr) {
        caches[expr] = setOf(expr.sort.accept(sortPreferredValuesProvider)(expr))
    }

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> =
        computeSideEffect(expr) {
            val nestedConstraints = expr.args.flatMapTo(mutableSetOf(), ::provide)
            val selfConstraint = expr.sort.accept(sortPreferredValuesProvider)(expr)

            caches[expr] = nestedConstraints + selfConstraint
        }

    private fun <Sort : USort> transformAppIfPossible(expr: UExpr<Sort>): UExpr<Sort> =
        if (expr is KApp<Sort, *>) transformApp(expr) else transformExpr(expr)

    // endregion

    // region USymbol specific methods

    override fun <Sort : USort> transform(expr: USymbol<Sort>): UExpr<Sort> =
        error("You must override `transform` function in UExprTranslator for ${expr::class}")

    override fun <Sort : USort> transform(expr: URegisterReading<Sort>): UExpr<Sort> = transformExpr(expr)

    override fun <Sort : USort> transform(
        expr: UCollectionReading<*, *, *>,
    ): UExpr<Sort> = error("You must override `transform` function in UExprTranslator for ${expr::class}")

    override fun <Sort : USort> transform(
        expr: UMockSymbol<Sort>,
    ): UExpr<Sort> = error("You must override `transform` function in UExprTranslator for ${expr::class}")

    override fun <Method, Sort : USort> transform(
        expr: UIndexedMethodReturnValue<Method, Sort>,
    ): UExpr<Sort> = transformAppIfPossible(expr)

    override fun transform(
        expr: UConcreteHeapRef,
    ): UExpr<UAddressSort> = error("Illegal operation since UConcreteHeapRef must not be translated into a solver")

    override fun transform(expr: UNullRef): UExpr<UAddressSort> = expr

    override fun transform(expr: UIsSubtypeExpr<Type>): UBoolExpr = expr

    override fun transform(expr: UIsSupertypeExpr<Type>): UBoolExpr = expr

    override fun <Field, Sort : USort> transform(expr: UInputFieldReading<Field, Sort>): UExpr<Sort> =
        readingWithSingleArgumentTransform(expr, expr.address)

    override fun <Sort : USort> transform(expr: UAllocatedArrayReading<Type, Sort>): UExpr<Sort> =
        readingWithSingleArgumentTransform(expr, expr.index)

    override fun <Sort : USort> transform(
        expr: UInputArrayReading<Type, Sort>,
    ): UExpr<Sort> = readingWithTwoArgumentsTransform(expr, expr.index, expr.address)

    override fun transform(
        expr: UInputArrayLengthReading<Type>,
    ): USizeExpr = computeSideEffect(expr) {
        with(expr.ctx) {
            val addressIsNull = provide(expr.address)
            val arraySize = mkBvSignedLessOrEqualExpr(expr, PREFERRED_MAX_ARRAY_SIZE.toBv())

            caches[expr] = addressIsNull + arraySize
        }
    }

    override fun <KeySort : USort, Sort : USort, Reg : Region<Reg>> transform(
        expr: UAllocatedMapReading<Type, KeySort, Sort, Reg>
    ): UExpr<Sort> = readingWithSingleArgumentTransform(expr, expr.key)

    override fun <KeySort : USort, Sort : USort, Reg : Region<Reg>> transform(
        expr: UInputMapReading<Type, KeySort, Sort, Reg>
    ): UExpr<Sort> = readingWithTwoArgumentsTransform(expr, expr.key, expr.address)

    override fun <Sort : USort> transform(
        expr: UAllocatedRefMapWithInputKeysReading<Type, Sort>
    ): UExpr<Sort> = readingWithSingleArgumentTransform(expr, expr.keyRef)

    override fun <Sort : USort> transform(
        expr: UInputRefMapWithAllocatedKeysReading<Type, Sort>
    ): UExpr<Sort> = readingWithSingleArgumentTransform(expr, expr.mapRef)

    override fun <Sort : USort> transform(
        expr: UInputRefMapWithInputKeysReading<Type, Sort>
    ): UExpr<Sort> = readingWithTwoArgumentsTransform(expr, expr.mapRef, expr.keyRef)

    override fun transform(
        expr: UInputMapLengthReading<Type>
    ): USizeExpr = computeSideEffect(expr) {
        with(expr.ctx) {
            val addressConstraints = provide(expr.address)
            val mapLength = mkBvSignedLessOrEqualExpr(expr, PREFERRED_MAX_ARRAY_SIZE.toBv())

            caches[expr] = addressConstraints + mapLength
        }
    }

    private fun <Sort : USort> readingWithSingleArgumentTransform(
        expr: UCollectionReading<*, *, Sort>,
        arg: UExpr<*>,
    ): UExpr<Sort> = computeSideEffect(expr) {
        val argConstraint = provide(arg)
        val selfConstraint = expr.sort.accept(sortPreferredValuesProvider)(expr)

        caches[expr] = argConstraint + selfConstraint
    }

    private fun <Sort : USort> readingWithTwoArgumentsTransform(
        expr: UCollectionReading<*, *, Sort>,
        arg0: UExpr<*>,
        arg1: UExpr<*>,
    ): UExpr<Sort> = computeSideEffect(expr) {
        val constraints = mutableSetOf<UBoolExpr>()

        constraints += provide(arg0)
        constraints += provide(arg1)
        constraints += expr.sort.accept(sortPreferredValuesProvider)(expr)

        caches[expr] = constraints
    }

    // region KExpressions

    override fun <T : KBvSort> transform(expr: KBvSignedLessOrEqualExpr<T>): KExpr<KBoolSort> = with(expr.ctx) {
        computeSideEffect(expr) {
            val selfConstraint = mkEq(expr.arg0, expr.arg1)
            caches[expr] = mutableSetOf(selfConstraint) + provide(expr.arg0) + provide(expr.arg1)
        }
    }

    // endregion

    private inline fun <T : USort> computeSideEffect(
        expr: UExpr<T>,
        operationWithSideEffect: () -> Unit,
    ): UExpr<T> {
        operationWithSideEffect()
        return expr
    }

    companion object {
        const val PREFERRED_MAX_ARRAY_SIZE = 10
    }
}

private class SortPreferredValuesProvider : KSortVisitor<(KExpr<*>) -> KExpr<KBoolSort>> {
    private val caches: MutableMap<USort, (KExpr<*>) -> KExpr<KBoolSort>> = mutableMapOf()

    override fun <S : KBvSort> visit(sort: S): (KExpr<*>) -> KExpr<KBoolSort> = caches.getOrPut(sort) {
        with(sort.ctx) {
            when (sort) {
                is KBv1Sort -> { expr -> 1.toBv(sort) eq expr.asExpr(sort) }
                else -> {
                    val (minValue, maxValue) = if (sort.sizeBits < 16u) {
                        SMALL_INT_MIN_VALUE to SMALL_INT_MAX_VALUE
                    } else {
                        INT_MIN_VALUE to INT_MAX_VALUE
                    }

                    { expr -> createBvBounds(lowerBound = minValue, upperBound = maxValue, expr) }
                }
            }
        }
    }

    private fun createBvBounds(
        lowerBound: Int,
        upperBound: Int,
        expr: UExpr<*>,
    ): UBoolExpr = with(expr.ctx) {
        val sort = expr.sort as UBvSort
        mkAnd(
            mkBvSignedLessOrEqualExpr(lowerBound.toBv(sort), expr.asExpr(sort)),
            mkBvSignedGreaterOrEqualExpr(upperBound.toBv(sort), expr.asExpr(sort))
        )
    }

    override fun <S : KFpSort> visit(sort: S): (KExpr<*>) -> KExpr<KBoolSort> = caches.getOrPut(sort) {
        when (sort) {
            is KFp32Sort -> { expr -> createFpBounds(expr) }
            is KFp64Sort -> { expr -> createFpBounds(expr) }
            else -> { expr -> createFpBounds(expr) }
        }
    }

    // TODO find a better way to limit fp values
    private fun createFpBounds(expr: UExpr<*>): UBoolExpr = with(expr.uctx) {
        val sort = expr.sort as KFpSort
        mkAnd(
            mkFpLessOrEqualExpr(FP_MIN_VALUE.toFp(sort), expr.asExpr(sort)),
            mkFpGreaterOrEqualExpr(FP_MAX_VALUE.toFp(sort), expr.asExpr(sort))
        )
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> visit(
        sort: KArray2Sort<D0, D1, R>,
    ): (KExpr<*>) -> KExpr<KBoolSort> = sort.range.accept(this)

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> visit(
        sort: KArray3Sort<D0, D1, D2, R>,
    ): (KExpr<*>) -> KExpr<KBoolSort> = sort.range.accept(this)

    override fun <R : KSort> visit(
        sort: KArrayNSort<R>,
    ): (KExpr<*>) -> KExpr<KBoolSort> = sort.range.accept(this)

    override fun <D : KSort, R : KSort> visit(
        sort: KArraySort<D, R>,
    ): (KExpr<*>) -> KExpr<KBoolSort> = sort.range.accept(this)

    override fun visit(sort: KBoolSort): (KExpr<*>) -> KExpr<KBoolSort> = { it.asExpr(sort) }

    override fun visit(sort: KFpRoundingModeSort): (KExpr<*>) -> KExpr<KBoolSort> =
        caches.getOrPut(sort) {
            with(sort.uctx) {
                // TODO double check it
                { expr -> mkFpRoundingModeExpr(KFpRoundingMode.RoundNearestTiesToEven) eq expr.asExpr(sort) }
            }
        }

    override fun visit(sort: KIntSort): (KExpr<*>) -> KExpr<KBoolSort> =
        caches.getOrPut(sort) {
            with(sort.uctx) {
                { expr ->
                    mkAnd(
                        mkArithLe(INT_MIN_VALUE.expr, expr.asExpr(sort)),
                        mkArithGe(INT_MAX_VALUE.expr, expr.asExpr(sort))
                    )
                }
            }
        }

    // TODO find a better way to limit real values
    override fun visit(sort: KRealSort): (KExpr<*>) -> KExpr<KBoolSort> =
        caches.getOrPut(sort) {
            with(sort.uctx) {
                { expr ->
                    mkAnd(
                        mkArithLe(mkRealNum(INT_MIN_VALUE), expr.asExpr(sort)),
                        mkArithGe(mkRealNum(INT_MAX_VALUE), expr.asExpr(sort))
                    )
                }
            }
        }

    override fun visit(sort: KUninterpretedSort): (KExpr<*>) -> KExpr<KBoolSort> =
        caches.getOrPut(sort) {
            with(sort.uctx) {
                if (sort === addressSort) {
                    { expr -> mkHeapRefEq(nullRef, expr.asExpr(sort)) }
                } else {
                    { _ -> trueExpr }
                }
            }
        }

    companion object {
        const val SMALL_INT_MIN_VALUE = -8
        const val SMALL_INT_MAX_VALUE = 8

        const val INT_MIN_VALUE = -256
        const val INT_MAX_VALUE = 256

        const val FP_MIN_VALUE = -256.0f
        const val FP_MAX_VALUE = 256.0f
    }
}
