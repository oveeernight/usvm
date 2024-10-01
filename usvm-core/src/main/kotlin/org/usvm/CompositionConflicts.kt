package org.usvm

import io.ksmt.expr.KApp
import io.ksmt.expr.KExpr
import io.ksmt.sort.KSort
import org.usvm.collection.array.UAllocatedArrayReading
import org.usvm.collection.array.UInputArrayReading
import org.usvm.collection.array.length.UInputArrayLengthReading
import org.usvm.collection.field.UInputFieldReading
import org.usvm.collection.map.length.UInputMapLengthReading
import org.usvm.collection.map.primitive.UAllocatedMapReading
import org.usvm.collection.map.primitive.UInputMapReading
import org.usvm.collection.map.ref.UAllocatedRefMapWithInputKeysReading
import org.usvm.collection.map.ref.UInputRefMapWithAllocatedKeysReading
import org.usvm.collection.map.ref.UInputRefMapWithInputKeysReading
import org.usvm.collection.set.primitive.UAllocatedSetReading
import org.usvm.collection.set.primitive.UInputSetReading
import org.usvm.collection.set.ref.UAllocatedRefSetWithInputElementsReading
import org.usvm.collection.set.ref.UInputRefSetWithAllocatedElementsReading
import org.usvm.collection.set.ref.UInputRefSetWithInputElementsReading
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.memory.UReadOnlyMemory
import org.usvm.memory.USymbolicCollection
import org.usvm.memory.USymbolicCollectionId
import org.usvm.memory.USymbolicCollectionKeyInfo
import org.usvm.regions.Region
import kotlin.math.exp

class ConflictsComposer<Type, USizeSort : USort>(
    ctx: UContext<USizeSort>,
    private val mmmmmemory: UReadOnlyMemory<Type>,
    override val ownership: MutabilityOwnership
) : UComposer<Type, USizeSort>(ctx, mmmmmemory, ownership) {

    override fun <T : KSort> transformExpr(expr: KExpr<T>): KExpr<T> {
        return super.transformExpr(expr)
    }

    override val memory: UReadOnlyMemory<Type>
        get() = mmmmmemory

    fun getConflictingOwnerships(expr: UBoolExpr): List<MutabilityOwnership> {
        val atoms = conflictingAtoms(expr)

        return atoms.flatMap { conflict -> (conflict as KApp<*, *>).args }
            .mapNotNull { it as? UCollectionReading<*,*,*> }
            .map { it.readingConflict(this) }
    }


    fun conflictingAtoms(expr: UBoolExpr): List<UBoolExpr> {
        val conflictingAtoms = mutableListOf<UBoolExpr>()
        if (compose(expr).isFalse) {
            collectConflictingExpressions(expr, conflictingAtoms, ctx.falseExpr)
        }
        return conflictingAtoms
    }

    private fun collectConflictingExpressions(
        expr: UBoolExpr,
        acc: MutableList<UBoolExpr>,
        conflictingValue: UBoolExpr,
    ) {
        when (expr) {
            is UAndExpr -> expr.collectConflictingArgs(conflictingValue, this)
                .forEach { collectConflictingExpressions(it, acc, conflictingValue) }

            is UOrExpr -> expr.collectConflictingArgs(conflictingValue, this)
                .forEach { collectConflictingExpressions(it, acc, conflictingValue) }

            is UNotExpr -> {
                val arg = expr.arg
                val invertedConflictingValue = expr.ctx.mkNot(conflictingValue)
                when (arg) {
                    is UAndExpr -> arg.collectConflictingArgs(invertedConflictingValue, this)
                        .forEach { collectConflictingExpressions(it, acc, invertedConflictingValue) }

                    is UOrExpr -> arg.collectConflictingArgs(invertedConflictingValue, this)
                        .forEach { collectConflictingExpressions(it, acc, invertedConflictingValue) }

                    else -> acc.add(expr)
                }
            }

            else -> acc.add(expr)
        }
    }

    override fun <Sort : KSort> transformExpr(expr: KExpr<Sort>) : KExpr<Sort> {

    }

    private fun UAndExpr.collectConflictingArgs(conflictingValue: UBoolExpr, composer: UComposer<*, *>): List<UBoolExpr> =
        when {
            // at least 1 arg is conflicting
            conflictingValue.isFalse -> this.args.filter { composer.compose(it).isFalse }
            // all args are conflicting
            conflictingValue.isTrue -> this.args

            else -> error("Unexpected conflicting value $conflictingValue")
        }

    private fun UOrExpr.collectConflictingArgs(conflictingValue: UBoolExpr, composer: UComposer<*, *>) : List<UBoolExpr> =
        when {
            // all args are conflicting
            conflictingValue.isFalse -> this.args
            // at least 1 arg is conflicting
            conflictingValue.isTrue -> this.args.filter { composer.compose(it).isTrue }

            else -> error("Unexpected conflicting value $conflictingValue")
        }

    private fun <CollectionId : USymbolicCollectionId<Key, Sort, CollectionId>, Key, Sort : USort> getConflictFromCollectionReading(
        expr: UCollectionReading<CollectionId, Key, Sort>,
        key: Key,
    ): MutabilityOwnership = with(expr) {
        val keyInfo = collection.collectionId.keyInfo()
        val mappedKey = keyInfo.mapKey(key, this@ConflictsComposer)
        val includedUpdates = collection.updates.read(mappedKey, this@ConflictsComposer)
        return includedUpdates.single().ownership
    }

    fun getReadingConflict(expr: UInputArrayLengthReading<Type, USizeSort>): MutabilityOwnership =
        getConflictFromCollectionReading(expr, expr.address)

    fun <Sort : USort> getReadingConflict(expr: UInputArrayReading<Type, Sort, USizeSort>) {
        getConflictFromCollectionReading(expr, expr.address to expr.index)
    }
    fun <Sort : USort> getReadingConflict(expr: UAllocatedArrayReading<Type, Sort, USizeSort>) =
        getConflictFromCollectionReading(expr, expr.index)

    fun <Field, Sort : USort> getReadingConflict(expr: UInputFieldReading<Field, Sort>) =
        getConflictFromCollectionReading(expr, expr.address)

    fun <KeySort : USort, Sort : USort, Reg : Region<Reg>> getReadingConflict(expr: UAllocatedMapReading<Type, KeySort, Sort, Reg>) =
        getConflictFromCollectionReading(expr, expr.key)

    fun <KeySort : USort, Sort : USort, Reg : Region<Reg>> getReadingConflict(
        expr: UInputMapReading<Type, KeySort, Sort, Reg>
    ) = getConflictFromCollectionReading(expr, expr.address to expr.key)

    fun <Sort : USort> getReadingConflict(expr: UAllocatedRefMapWithInputKeysReading<Type, Sort>) =
        getConflictFromCollectionReading(expr, expr.keyRef)

    fun <Sort : USort> getReadingConflict(
        expr: UInputRefMapWithAllocatedKeysReading<Type, Sort>
    ) = getConflictFromCollectionReading(expr, expr.mapRef)

    fun <Sort : USort> getReadingConflict(
        expr: UInputRefMapWithInputKeysReading<Type, Sort>
    ) = getConflictFromCollectionReading(expr, expr.mapRef to expr.keyRef)

    fun getReadingConflict(expr: UInputMapLengthReading<Type, USizeSort>) =
        getConflictFromCollectionReading(expr, expr.address)

    fun <ElemSort : USort, Reg : Region<Reg>> getReadingConflict(
        expr: UAllocatedSetReading<Type, ElemSort, Reg>
    ) = getConflictFromCollectionReading(expr, expr.element)

    fun <ElemSort : USort, Reg : Region<Reg>> getReadingConflict(expr: UInputSetReading<Type, ElemSort, Reg>) =
        getConflictFromCollectionReading(expr, expr.address to expr.element)

    fun getReadingConflict(expr: UAllocatedRefSetWithInputElementsReading<Type>) =
        getConflictFromCollectionReading(expr, expr.elementRef)

    fun getReadingConflict(expr: UInputRefSetWithAllocatedElementsReading<Type>) =
        getConflictFromCollectionReading(expr, expr.setRef)

    fun getReadingConflict(expr: UInputRefSetWithInputElementsReading<Type>) =
        getConflictFromCollectionReading(expr, expr.setRef to expr.elementRef)
}

@Suppress("UNCHECKED_CAST")
fun <Type, USizeSort : USort> ConflictsComposer<*, *>.asTypedComposer(): ConflictsComposer<Type, USizeSort> =
    this as ConflictsComposer<Type, USizeSort>

sealed class ConflictResolvingResult

class UnknownResult : ConflictResolvingResult()

class ResolvedConflict(val ownership: MutabilityOwnership) : ConflictResolvingResult()
