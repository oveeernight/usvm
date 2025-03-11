package org.usvm.machine.state

import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
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
import org.usvm.org.usvm.expressions.addCut
import org.usvm.org.usvm.expressions.mkCombine
import org.usvm.org.usvm.expressions.mkSlice
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
                readExprUnsafe(i.elem, i.start, i.end, pos, posIsStable = false) }
            val filtered = slices.filterIsInstance<Slice<Sort>>()
            require(slices.size == filtered.size)
            return ctx.mkCombine(filtered, sightType)
        }

        private fun readExprUnsafe(expr: UExpr<Sort>, start: UExpr<UBvSort>, end: UExpr<UBvSort>, pos: UExpr<UBvSort>, posIsStable: Boolean): List<UExpr<Sort>> {
            return when (expr) {
                is Slice<Sort> -> {
                    val cut = Cut(start, end, pos, posIsStable)
                    val newExpr = ctx.addCut(expr, cut)
                    listOf(newExpr)
                }
                is Combine<Sort> -> {
                    val slices = expr.slices
                    val read = slices.flatMap { readExprUnsafe(it.expr, start, end, pos, posIsStable) }
                    read
                }
                else -> {
                    val cut = Cut(start, end, pos, posIsStable)
                    val cuts = LinkedList<Cut>().also { it.add(cut) }
                    val slice = ctx.mkSlice(expr, cuts)
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
                val fstAffectedIdx = mkBvSignedDivExpr(offset, mkBv(elementSize, offset.sort))
                var currentOffset = mkBvMulExpr(elemSizeBv, fstAffectedIdx)
                (0..countToRead).map {
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
