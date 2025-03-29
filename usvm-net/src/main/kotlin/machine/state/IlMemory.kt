package org.usvm.machine.state

import io.ksmt.expr.KBitVec32Value
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
import org.usvm.machine.USizeSort
import org.usvm.machine.ilctx
import org.usvm.machine.write
import java.util.LinkedList
import kotlin.math.max

// TODO get rid of isArray


sealed interface IlLocation<Sort: USort> : ULocation<Sort, IlType>

class IlHeapLocation<Sort : USort>(
    val ref: UHeapRef,
    override val sort: Sort,
    override val type: IlType,
    val isArray: Boolean
) : IlLocation<Sort> {
    override fun affectedKeys(offset: UExpr<UBvSort>, viewType: IlType): List<AffectedKey<IlType, out USort>> {
        val resolver = UnsafeKeysResolver()
        return if (isArray) {
            resolver.getAffectedIndices(ref, type, sort, offset, viewType)
        } else {
            resolver.getAffectedFields(ref, type, offset, viewType)
        }
    }
}

class IlStackLocation<Sort: USort>(val key: URegisterStackLValue<Sort>, override val type: IlType): IlLocation<Sort> {
    override fun affectedKeys(offset: UExpr<UBvSort>, viewType: IlType): List<AffectedKey<IlType, out USort>> =
        with(key.sort.ctx) {
        val size : UExpr<UBvSort> = mkBv(type.size, bv32Sort)
        val ak = AffectedRegister(key, type, offset, size)
        return listOf(ak)
    }
    override val sort = key.sort
}
class IlStaticLocation<Sort: USort>(override val sort: Sort, override val type: IlType): IlLocation<Sort> {
    override fun affectedKeys(offset: UExpr<UBvSort>, viewType: IlType): List<AffectedKey<IlType, out USort>> {
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
    override fun readUnsafe(lvalue: UnsafeLValue<out USort, IlType>): UExpr<out USort> {
        val affectedKeys = lvalue.location.affectedKeys(lvalue.offset, lvalue.sightType)
        val slices = affectedKeys.flatMap { ak -> ak.read(this) }
        val filtered = slices.filterIsInstance<Slice<out USort>>()
        assert(slices.size == filtered.size)
        val sort = lvalue.offset.ilctx.typeToSort(lvalue.sightType)
        val combine = ctx.mkCombine(filtered, sort, lvalue.sightType)
        return combine
    }

    override fun writeUnsafe(lvalue: UnsafeLValue<out USort, IlType>, value: UExpr<out USort>, valueType: IlType) {
        val affectedKeys = lvalue.location.affectedKeys(lvalue.offset, lvalue.sightType)
        affectedKeys.forEach { ak ->
            ak.write(this, value, valueType)
        }
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

private data class AffectedIndex<Sort: USort>(
    override val key: UArrayIndexLValue<IlType, Sort, USizeSort>,
    override val start: UExpr<UBvSort>, override val end: UExpr<UBvSort>
) : AffectedKey<IlType, Sort> {
    override fun read(memory: UnsafeMemory<IlType, *>): List<UExpr<out USort>> {
        val value = memory.read(key)
        val pos = start.ctx.mkBvNegationExpr(start)
        return readExprUnsafe(value, key.arrayType, start, end, pos, posIsStable = false)
    }

    override fun write(memory: UnsafeMemory<IlType, *>, value: UExpr<out USort>, valueType: IlType) {
        val oldValue = memory.read(key)
        val newValue = writeExprUnsafe(oldValue, key.arrayType, value, valueType, start)
        memory.write(key, newValue.cast(), guard = start.ctx.trueExpr)
    }
}

private data class AffectedField<Sort : USort>(
    override val key: UFieldLValue<IlField, Sort>, val fieldOffset: UExpr<UBvSort>,
    override val start: UExpr<UBvSort>, override val end: UExpr<UBvSort>
) : AffectedKey<IlType, Sort> {
    override fun read(memory: UnsafeMemory<IlType, *>): List<UExpr<out USort>> {
        val value = memory.read(key)
        return readExprUnsafe(value, key.field.fieldType, start, end, start, posIsStable = false)
    }

    override fun write(memory: UnsafeMemory<IlType, *>, value: UExpr<out USort>, valueType: IlType) {
        TODO("Not yet implemented")
    }
}

private data class AffectedRegister<Sort : USort>(
    override val key: ULValue<*, Sort>,
    val type: IlType,
    override val start: UExpr<UBvSort>,
    override val end: UExpr<UBvSort>
) : AffectedKey<IlType, Sort> {
    override fun read(memory: UnsafeMemory<IlType, *>): List<UExpr<out USort>> {
        val value = memory.read(key)
        val pos = start.ctx.mkBvNegationExpr(start)
        return readExprUnsafe(value, type, start, end, pos, posIsStable = false)
    }

    override fun write(memory: UnsafeMemory<IlType, *>, value: UExpr<out USort>, valueType: IlType) {
        val oldValue = memory.read(key)
        val newValue = writeExprUnsafe(oldValue, type, value, valueType, start)
        memory.write(key, newValue)
    }

}


private class UnsafeKeysResolver {
    // TODO possible index out of bounds because of extra + 1
    fun <Sort : USort> getAffectedIndices(
        arrayRef: UHeapRef,
        elementType: IlType,
        elemSort: Sort,
        offset: UExpr<UBvSort>,
        sightType: IlType
    ): List<AffectedKey<IlType, Sort>> {
        val viewSize = sightType.size
        val elementSize = elementType.size
        val concreteOffset = offset as? KBitVec32Value
        val countToRead =
            if (concreteOffset != null) {
                val offsetInsideElement = concreteOffset.intValue % elementSize
                val finalByte = concreteOffset.intValue + viewSize
                if (finalByte % elementSize == 0 && offsetInsideElement == 0) viewSize / elementSize
                else {
                    // it is possible to affect 2 elements
                    // consider case when we write short value in integer array with offset 3
                    // or writing 20 byte struct in long array with offset 6
                    // extra +1 because of this case. if it is no the case, it will be translated to empty slice
                    viewSize / elementSize + 1 + if (viewSize % elementSize > 1) 1 else 0
                }
            } else {
                viewSize / elementSize + 1 + if (viewSize % elementSize > 1) 1 else 0
            }


        // TODO check bounds, check if simplify current offset will improve perfomance
        return with(offset.ctx) {
            val viewSizeBv: UExpr<UBvSort> = mkBv(viewSize, bv32Sort)
            val elemSizeBv: UExpr<UBvSort> = mkBv(elementSize, bv32Sort)
            val fstAffectedIdx = mkBvSignedDivExpr(offset, elemSizeBv)
            var currentOffset = mkBvMulExpr(elemSizeBv, fstAffectedIdx)
            (0..<countToRead).map {
                val idx = mkBvAddExpr(fstAffectedIdx, mkBv(it, fstAffectedIdx.sort))
                val key = UArrayIndexLValue(elemSort, arrayRef, idx, elementType)
                val start = mkBvSubExpr(offset, currentOffset)
                val end = mkBvAddExpr(start, viewSizeBv)
                currentOffset = mkBvAddExpr(currentOffset, elemSizeBv)
                AffectedIndex(key.cast(), start, end)
            }
        }
    }

//    fun commonReadFieldsUnsafe(
//        type: IlType,
//        start: UExpr<UBvSort>,
//        end: UExpr<UBvSort>,
//        pos: UExpr<UBvSort>,
//        posIsStable: Boolean,
//        readField: (IlField) -> UExpr<out USort>
//    ): List<UExpr<out USort>> {
//        val affectedFields = getAffectedFields(type, start, end, readField)
//        val slices = affectedFields.flatMap { (field, offset, value, s, e) ->
//            val p = start.ctx.mkBvAddExpr(offset, pos)
//            readExprUnsafe(value, field.fieldType, s, e, p, posIsStable)
//        }
//        return slices
//    }

    // TODO optimize if start (so the end is) are concrete
    fun getAffectedFields(
        ref: UHeapRef,
        type: IlType,
        offset: UExpr<UBvSort>,
        viewType: IlType,
    ): List<AffectedKey<IlType, out USort>> {
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
                AffectedField(key, fieldOffset, affectedStart, affectedEnd )
            }
        }
    }
}

private fun <Sort: USort> readExprUnsafe(
    expr: UExpr<Sort>,
    exprType: IlType,
    start: UExpr<UBvSort>,
    end: UExpr<UBvSort>,
    pos: UExpr<UBvSort>,
    posIsStable: Boolean
): List<UExpr<out USort>> {
    return when (expr) {
        is Slice<Sort> -> {
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
    val valueSlices = readExprUnsafe(
        value,
        valueType,
        mkBvNegationExpr(start),
        mkBvSubExpr(exprSize, start),
        start,
        posIsStable = false
    )
    val slices = listOf(leftUnaffected, valueSlices, rightUnaffected).flatten()
    val filtered = slices.filterIsInstance<Slice<Sort>>()
    assert(slices.size == filtered.size)
    val exprSort = expr.ilctx.typeToSort(exprType)
    mkCombine(filtered, exprSort, exprType)
}

private val zeroField : IlField
    get() = TODO()
