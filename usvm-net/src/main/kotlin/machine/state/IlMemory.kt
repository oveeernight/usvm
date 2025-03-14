package org.usvm.machine.state

import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.IlFieldDto
import org.jacodb.api.net.generated.models.IlTypeDto
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlFieldImpl
import org.jacodb.api.net.ilinstances.impl.IlTypeImpl
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.constraints.UTypeConstraints
import org.usvm.expressions.Combine
import org.usvm.expressions.Cut
import org.usvm.expressions.Slice
import org.usvm.expressions.size
import org.usvm.machine.USizeSort
import org.usvm.memory.*
import org.usvm.expressions.addCut
import org.usvm.expressions.mkCombine
import org.usvm.expressions.mkSlice
import java.util.*

class IlMemory(
    override val ctx: UContext<*>,
    ownership: MutabilityOwnership,
    types: UTypeConstraints<IlType>,
    stack: URegistersStack = URegistersStack(),
    mocks: UIndexedMocker<IlMethod> = UIndexedMocker(),
    regions: UPersistentHashMap<UMemoryRegionId<*, *>, UMemoryRegion<*, *>> = persistentHashMapOf()
) : UnsafeMemory<IlType, IlMethod>(ctx, ownership, types, stack, mocks, regions) {
    override fun <Sort : USort> readUnsafe(lvalue: UnsafeLValue<Sort>): UExpr<Sort> {
        TODO("Not yet implemented")
    }

    override fun <Sort : USort> writeUnsafe(lvalue: UnsafeLValue<Sort>, value: UExpr<Sort>) {
        TODO("Not yet implemented")
    }

    private inner class UnsafeKeysResolver<Key, Sort : USort> {
        private fun readArrayUnsafe(base: UArrayIndexLValue<IlType, Sort, USizeSort>, offset: UExpr<UBvSort>, sightType: IlType): UExpr<Sort> {
            val affectedIndices = getAffectedIndices(base, offset, sightType)
            val slices = affectedIndices.flatMap { i ->
                val pos = ctx.mkBvNegationExpr(i.start)
                readExprUnsafe(i.elem, base.arrayType, i.start, i.end, pos, posIsStable = false) }
            val filtered = slices.filterIsInstance<Slice<Sort>>()
            require(slices.size == filtered.size)
            return ctx.mkCombine(filtered, sightType)
        }

        private fun writeArrayUnsafe(
            base: UArrayIndexLValue<IlType, Sort, USizeSort>,
            offset: UExpr<UBvSort>,
            value: UExpr<Sort>,
            valueType: IlType
        ) {
            val affectedIndices = getAffectedIndices(base, offset, valueType)
            affectedIndices.forEach { i ->
                val key = UArrayIndexLValue(base.sort, base.ref, i.idx, base.arrayType)
                val newValue = writeExprUnsafe(i.elem, base.arrayType, value, valueType, i.start)
                this@IlMemory.write(key, newValue, guard = ctx.trueExpr)
            }
        }

        // TODO optimizations based on type and size
        private fun writeExprUnsafe(
            expr: UExpr<Sort>,
            exprType: IlType,
            value: UExpr<Sort>,
            valueType: IlType,
            start: UExpr<UBvSort>,
        ) = with (ctx) {
            val exprSize : UExpr<UBvSort> = mkBv(exprType.size, bv32Sort)
            val valueSize : UExpr<UBvSort> = mkBv(valueType.size, bv32Sort)
            val zero : UExpr<UBvSort> = mkBv(0, bv32Sort)
            val leftUnaffected = readExprUnsafe(expr, exprType, zero, start, zero, posIsStable = true)
            val rightUnaffectedStart = mkBvAddExpr(start, valueSize)
            val rightUnaffected =
                readExprUnsafe(expr, exprType, rightUnaffectedStart, exprSize, rightUnaffectedStart, posIsStable = true)
            val valueStart = mkBvNegationExpr(start)
            val valueSlices = readExprUnsafe(
                value,
                valueType,
                valueStart,
                mkBvSubExpr(exprSize, start),
                start,
                posIsStable = false
            )
            val slices = listOf(leftUnaffected, valueSlices,  rightUnaffected).flatten()
            val filtered = slices.filterIsInstance<Slice<Sort>>()
            assert(slices.size == filtered.size)
            mkCombine(filtered, exprType)
        }

        private fun readExprUnsafe(
            expr: UExpr<Sort>,
            exprType: IlType,
            start: UExpr<UBvSort>,
            end: UExpr<UBvSort>,
            pos: UExpr<UBvSort>,
            posIsStable: Boolean
        ): List<UExpr<Sort>> {
            return when (expr) {
                is Slice<Sort> -> {
                    val cut = Cut(start, end, pos, posIsStable)
                    val newExpr = ctx.addCut(expr, cut)
                    listOf(newExpr)
                }
                is Combine<Sort> -> {
                    val slices = expr.slices
                    slices.flatMap { readExprUnsafe(it.expr, exprType,  start, end, pos, posIsStable) }
                }
                else -> {
                    val cut = Cut(start, end, pos, posIsStable)
                    val cuts = LinkedList<Cut>().also { it.add(cut) }
                    val slice = ctx.mkSlice(expr, exprType, cuts)
                    listOf(slice)
                }
            }
        }

        private fun getAffectedIndices(
            base: UArrayIndexLValue<IlType, Sort, USizeSort>,
            offset: UExpr<UBvSort>,
            sightType: IlType
        ): List<AffectedIndex<Sort>> {
            val viewSize = sightType.size
            val elementSize = base.arrayType.size
            val countToRead =
                if (viewSize == 1) {
                    // it is not possible to affect 2 elements
                    // +1 because of offset can point to middle of element
                    viewSize / elementSize + 1
                } else {
                    // it is possible to affect 2 elements
                    // consider case when we write short value in integer array with offset 3
                    // extra +1 because of this case. if it is no the case, it will be translated to empty slice
                    viewSize / elementSize + 2
                }

            // TODO check bounds, check if simplify current offset will improve perfomance
            return with(offset.ctx) {
                val viewSizeBv : UExpr<UBvSort> = mkBv(viewSize, bv32Sort)
                val elemSizeBv : UExpr<UBvSort> = mkBv(elementSize, bv32Sort)
                val fstAffectedIdx = mkBvSignedDivExpr(offset, elemSizeBv)
                var currentOffset = mkBvMulExpr(elemSizeBv, fstAffectedIdx)
                (0..<countToRead).map {
                    val idx = mkBvAddExpr(fstAffectedIdx, mkBv(it, fstAffectedIdx.sort))
                    val key = UArrayIndexLValue(base.sort, base.ref, idx, base.arrayType)
                    val elem = read(key)
                    val start = mkBvSubExpr(offset, currentOffset)
                    val end = mkBvAddExpr(start, viewSizeBv)
                    currentOffset = mkBvAddExpr(currentOffset, elemSizeBv)
                    AffectedIndex(idx, elem, start, end)
                }
            }
        }

        private fun <Sort : USort> getAffectedFields(
            type: IlType,
            offset: UExpr<UBvSort>,
            viewSize: UExpr<UBvSort>
        ): List<AffectedField<Sort>> {

        }


//    fun convert(lValue: UnsafeLValue<Sort>) : List<ULValue<Key, Sort>> {
//        lValue.
//    }
    }

    override fun clone(
        typeConstraints: UTypeConstraints<IlType>,
        thisOwnership: MutabilityOwnership,
        cloneOwnership: MutabilityOwnership
    ): UnsafeMemory<IlType, IlMethod> =
        IlMemory(ctx, cloneOwnership, typeConstraints, stack.clone(), mocks.clone(), regions).also {
            it.ownership = thisOwnership
        }
}

private data class AffectedIndex<Sort : USort>(
    val idx: UExpr<UBvSort>, val elem: UExpr<Sort>,
    val start: UExpr<UBvSort>, val end: UExpr<UBvSort>
)

private data class AffectedField<Sort : USort>(
    val field: IlField, val value: UExpr<Sort>,
    val start: UExpr<UBvSort>, val end: UExpr<UBvSort>
)

private val IlField.offset : Int
    get() = TODO()

private val zeroField : IlField
    get() = TODO()
