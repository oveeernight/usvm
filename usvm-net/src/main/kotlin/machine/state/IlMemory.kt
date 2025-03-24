package org.usvm.machine.state

import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.constraints.UTypeConstraints
import org.usvm.expressions.Combine
import org.usvm.expressions.Cut
import org.usvm.expressions.Slice
import org.usvm.memory.*
import org.usvm.expressions.addCut
import org.usvm.expressions.mkCombine
import org.usvm.expressions.mkSlice
import org.usvm.machine.ilctx
import java.util.LinkedList
import kotlin.math.max

// TODO get rid of isArray

data class AffectedKey(val key: ULValue<*, *>, val start: UExpr<UBvSort>, val end: UExpr<UBvSort>)


sealed interface IlLocation<Sort: USort> : ULocation<Sort> {
    fun affectedKeys(offset: UExpr<UBvSort>, viewType: IlType) : List<AffectedKey>
}

class IlHeapLocation<Sort : USort>(
    val ref: UHeapRef,
    override val sort: Sort,
    val type: IlType,
    val isArray: Boolean
) : IlLocation<Sort> {
    override fun affectedKeys(offset: UExpr<UBvSort>, viewType: IlType): List<AffectedKey> {
        val resolver = UnsafeResolver()
        return if (isArray) {
            resolver.getAffectedIndices(ref, type, sort, offset, viewType)
        } else {
            resolver.getAffectedFields(ref, type, offset, viewType)
        }
    }

    override fun memoryRegionId(): UMemoryRegionId<*, *> {
        return if (isArray) {
            val key = UArrayIndexLValue(sort, ref, ref.ilctx.mkSizeExpr(0), type)
            key.memoryRegionId
        } else {
            val key = UFieldLValue(sort, ref, type)
            key.memoryRegionId
        }
    }
}

class IlStackLocation<Sort: USort>(val key: URegisterStackLValue<Sort>): IlLocation<Sort> {
    override fun affectedKeys(offset: UExpr<UBvSort>, viewType: IlType): List<AffectedKey> {
        TODO("Not yet implemented")
    }

    override val sort = key.sort
    override fun memoryRegionId(): UMemoryRegionId<*, *> = key.memoryRegionId
}
class IlStaticLocation<Sort: USort>(override val sort: Sort, val type: IlType): IlLocation<Sort> {
    override fun affectedKeys(offset: UExpr<UBvSort>, viewType: IlType): List<AffectedKey> {
        TODO("Not yet implemented")
    }

    override fun memoryRegionId(): UMemoryRegionId<*, *> {
        TODO("Not yet implemented")
    }
}

class IlMemory(
    ctx: UContext<*>,
    ownership: MutabilityOwnership,
    types: UTypeConstraints<IlType>,
    stack: URegistersStack = URegistersStack(),
    mocks: UIndexedMocker<IlMethod> = UIndexedMocker(),
    regions: UPersistentHashMap<UMemoryRegionId<*, *>, UMemoryRegion<*, *>> = persistentHashMapOf()
) : UnsafeMemory<IlType, IlMethod>(ctx, ownership, types, stack, mocks, regions) {

    override fun readUnsafe(lvalue: UnsafeLValue<out USort>): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun writeUnsafe(lvalue: UnsafeLValue<out USort>, value: UExpr<out USort>) {
        TODO("Not yet implemented")
    }

    override fun clone(
        typeConstraints: UTypeConstraints<IlType>,
        thisOwnership: MutabilityOwnership,
        cloneOwnership: MutabilityOwnership
    ): IlMemory =
        IlMemory(ctx, cloneOwnership, typeConstraints, stack.clone(), mocks.clone(), regions).also {
            it.ownership = thisOwnership
        }
}

private data class AffectedIndex<Sort : USort>(
    val idx: UExpr<UBvSort>, val elem: UExpr<Sort>,
    val start: UExpr<UBvSort>, val end: UExpr<UBvSort>
)

private data class AffectedField<Sort : USort>(
    val field: IlField, val fieldOffset: UExpr<UBvSort>, val value: UExpr<Sort>,
    val start: UExpr<UBvSort>, val end: UExpr<UBvSort>
)

private class UnsafeResolver {
    private fun <Sort : USort> readArrayUnsafe(
        arrayRef: UHeapRef,
        elemType: IlType,
        elemSort: USort,
        offset: UExpr<UBvSort>,
        sightType: IlType
    ): UExpr<Sort> {
        val affectedIndices = getAffectedIndices(arrayRef, elemType, elemSort, offset, sightType)
        val slices = affectedIndices.flatMap { i ->
            val pos = offset.ctx.mkBvNegationExpr(i.start)
            readExprUnsafe(i.elem, elemType, i.start, i.end, pos, posIsStable = false)
        }
        val filtered = slices.filterIsInstance<Slice<Sort>>()
        require(slices.size == filtered.size)
        return offset.ilctx.mkCombine(filtered, sightType)
    }

//    private fun writeArrayUnsafe(
//        arrayRef: UHeapRef,
//        elemType: IlType,
//        elemSort: USort,
//        offset: UExpr<UBvSort>,
//        value: UExpr<out USort>,
//        valueType: IlType
//    ) {
//        val affectedIndices = getAffectedIndices(arrayRef, elemType, elemSort, offset, valueType)
//        affectedIndices.forEach { i ->
//            val key = UArrayIndexLValue(elemSort, offset, i.idx, base.arrayType)
//            val newValue = writeExprUnsafe(i.elem, base.arrayType, value, valueType, i.start)
//            this@IlMemory.write(key, newValue.cast(), guard = offset.ilctx.trueExpr)
//        }
//    }

    // TODO optimizations based on type and size
    fun <Sort : USort> writeExprUnsafe(
        expr: UExpr<Sort>,
        exprType: IlType,
        value: UExpr<out USort>,
        valueType: IlType,
        start: UExpr<UBvSort>,
    ): UExpr<out USort> = with(expr.ilctx) {
        val exprSize: UExpr<UBvSort> = mkBv(exprType.size, bv32Sort)
        val valueSize: UExpr<UBvSort> = mkBv(valueType.size, bv32Sort)
        val zero: UExpr<UBvSort> = mkBv(0, bv32Sort)
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
        val slices = listOf(leftUnaffected, valueSlices, rightUnaffected).flatten()
        val filtered = slices.filterIsInstance<Slice<Sort>>()
        assert(slices.size == filtered.size)
        mkCombine(filtered, exprType)
    }

    fun readExprUnsafe(
        expr: UExpr<out USort>,
        exprType: IlType,
        start: UExpr<UBvSort>,
        end: UExpr<UBvSort>,
        pos: UExpr<UBvSort>,
        posIsStable: Boolean
    ): List<UExpr<out USort>> {
        return when (expr) {
            is Slice<out USort> -> {
                val cut = Cut(start, end, pos, posIsStable)
                val newExpr = expr.ilctx.addCut(expr, cut)
                listOf(newExpr)
            }

            is Combine<out USort> -> {
                val slices = expr.slices
                slices.flatMap { readExprUnsafe(it.expr, exprType, start, end, pos, posIsStable) }
            }

            else -> {
                val cut = Cut(start, end, pos, posIsStable)
                val cuts = LinkedList<Cut>().also { it.add(cut) }
                val slice = expr.ilctx.mkSlice(expr, exprType, cuts)
                listOf(slice)
            }
        }
    }

    // TODO possible index out of bounds because of extra + 1
    fun getAffectedIndices(
        arrayRef: UHeapRef,
        elementType: IlType,
        elemSort: USort,
        offset: UExpr<UBvSort>,
        sightType: IlType
    ): List<AffectedKey> {
        val viewSize = sightType.size
        val elementSize = elementType.size
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
            val viewSizeBv: UExpr<UBvSort> = mkBv(viewSize, bv32Sort)
            val elemSizeBv: UExpr<UBvSort> = mkBv(elementSize, bv32Sort)
            val fstAffectedIdx = mkBvSignedDivExpr(offset, elemSizeBv)
            var currentOffset = mkBvMulExpr(elemSizeBv, fstAffectedIdx)
            (0..<countToRead).map {
                val idx = mkBvAddExpr(fstAffectedIdx, mkBv(it, fstAffectedIdx.sort))
                val key = UArrayIndexLValue(elemSort, arrayRef, idx, elementSize)
                val start = mkBvSubExpr(offset, currentOffset)
                val end = mkBvAddExpr(start, viewSizeBv)
                currentOffset = mkBvAddExpr(currentOffset, elemSizeBv)
                AffectedKey(key, start, end)
            }
        }
    }

    fun commonReadFieldsUnsafe(
        type: IlType,
        start: UExpr<UBvSort>,
        end: UExpr<UBvSort>,
        pos: UExpr<UBvSort>,
        posIsStable: Boolean,
        readField: (IlField) -> UExpr<out USort>
    ): List<UExpr<out USort>> {
        val affectedFields = getAffectedFields(type, start, end, readField)
        val slices = affectedFields.flatMap { (field, offset, value, s, e) ->
            val p = start.ctx.mkBvAddExpr(offset, pos)
            readExprUnsafe(value, field.fieldType, s, e, p, posIsStable)
        }
        return slices
    }

    // TODO optimize if start (so the end is) are concrete
    fun getAffectedFields(
        ref: UHeapRef,
        type: IlType,
        offset: UExpr<UBvSort>,
        viewType: IlType,
    ): List<AffectedKey> {
        val end = with(ref.ctx) {
            mkBvAddExpr(offset, mkBv(viewType.size, bv32Sort))
        }
        val fieldTypeSize = type.size
        val fields = type.fields.sortedBy { it.offset }
        val fieldsWithZeros = LinkedList<IlField>()
        fields.foldRight(fieldTypeSize) { field, nextOffset ->
            val fieldOffset = field.offset
            val size = field.fieldType.size
            val extraZerosCount = max(0, nextOffset - fieldOffset + size)
            repeat((0..<extraZerosCount).count()) { fieldsWithZeros.addFirst(zeroField) }
            fieldsWithZeros.addFirst(field)
            fieldOffset
        }
        val extraStartZeros = fields[0].offset
        repeat((0..<extraStartZeros).count()) {
            fieldsWithZeros.addFirst(zeroField)
        }
        return with(offset.ilctx) {
            fieldsWithZeros.map {
                val key = UFieldLValue(typeToSort(it.fieldType), ref, it)
                val fieldOffset : UExpr<UBvSort> = mkBv(it.fieldType.size, bv32Sort)
                val affectedStart = mkBvSubExpr(offset, fieldOffset)
                val affectedEnd = mkBvSubExpr(end, offset)
                AffectedKey(key, affectedStart, affectedEnd)
            }
        }
    }
}

private val zeroField : IlField
    get() = TODO()
