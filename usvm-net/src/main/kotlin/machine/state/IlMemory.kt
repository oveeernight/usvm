package org.usvm.machine.state

import io.ksmt.expr.KBitVec32Value
import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.collections.immutable.getOrPut
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
import org.usvm.machine.*
import java.util.LinkedList
import kotlin.math.exp
import kotlin.math.max


class IlMemory(
    ctx: UContext<*>,
    ownership: MutabilityOwnership,
    types: UTypeConstraints<IlType>,
    private val callStack: UCallStack<IlMethod, IlStmt>,
    stack: URegistersStack = IlRegistersStack(),
    mocks: UIndexedMocker<IlMethod> = UIndexedMocker(),
    regions: UPersistentHashMap<UMemoryRegionId<*, *>, UMemoryRegion<*, *>> = persistentHashMapOf()
) : UnsafeMemory<IlType, IlMethod>(ctx, ownership, types, stack, mocks, regions) {
    @Suppress("UNCHECKED_CAST")
    override fun <Key, Sort : USort> getRegion(regionId: UMemoryRegionId<Key, Sort>): UMemoryRegion<Key, Sort> {
        if (regionId is IlRegisterStackId) return stack as UMemoryRegion<Key, Sort>

        val (updatedRegions, region) = regions.getOrPut(regionId, ownership) { regionId.emptyRegion() }
        regions = updatedRegions
        return region as UMemoryRegion<Key, Sort>
    }

    override fun <Key, Sort : USort> setRegion(
        regionId: UMemoryRegionId<Key, Sort>,
        newRegion: UMemoryRegion<Key, Sort>
    ) {
        if (regionId is StructsRegionId<*, *> && newRegion is StructsMemoryRegion<*, *>) {
            val structRegion = newRegion.structRegion
            val structRegionId = (regionId.structKey as ULValue<*, *>).memoryRegionId
            if (structRegionId is IlRegisterStackId) {
                check(structRegion === stack) { "Stack is mutable" }
                return
            }
            regions = regions.put(structRegionId, structRegion, ownership)

        }
        super.setRegion(regionId, newRegion)
    }

    override fun readUnsafe(lvalue: UnsafeLValue<out USort, IlType>): UExpr<out USort> {
        lvalue as IlPtr<*>
        return when (val base = lvalue.base) {
            is UArrayIndexLValue<*, *, *> -> {
                val elemType = base.arrayType as IlType
                val elemSort = base.sort.ilctx.typeToSort(elemType)
                val affectedValues = getAffectedIndices(base.ref, elemType, elemSort, lvalue.offset, lvalue.sightType )
                val slices = affectedValues.flatMap { (v, vt, s, e) ->
                    val pos = ctx.mkBvNegationExpr(s)
                    readExprUnsafe(v, vt, lvalue.sightType, s, e, pos, posIsStable = false)
                }
                val filtered = slices.filterIsInstance<Slice<out USort>>()
                val combineSort = elemSort.ilctx.typeToSort(lvalue.sightType)
                elemSort.ilctx.mkCombine(filtered, combineSort, lvalue.sightType)
            }
            is URegisterStackLValue<*> -> {
                val currMethod = callStack.lastMethod()
                val regType = currMethod.typeOfRegister(base.idx)
                val value = read(base)
                with(ctx) {
                    val pos : UExpr<UBvSort> = mkBv(0, bv32Sort)
                    val viewSize : UExpr<UBvSort> = mkBv(lvalue.sightType.size, bv32Sort)
                    val end = mkBvAddExpr(lvalue.offset, viewSize)
                    val slices = readExprUnsafe(value, regType, lvalue.sightType, lvalue.offset, end, pos, posIsStable = false)
                    val filtered = slices.filterIsInstance<Slice<out USort>>()
                    mkCombine(filtered, end.ilctx.typeToSort(lvalue.sightType), lvalue.sightType)
                }
            }

            is UFieldLValue<*, *> -> with(lvalue.offset.ilctx) {
                val type = base.field as IlType
                val pos : UExpr<UBvSort> = ctx.mkBv(0, ctx.bv32Sort)
                val slices = commonReadFields(type.declaringType!!, lvalue.offset, pos, lvalue.sightType) { field ->
                    read(UFieldLValue(lvalue.sort.ilctx.typeToSort(field.fieldType), base.ref, field))
                }
                val filtered = slices.filterIsInstance<Slice<out USort>>()
                assert(slices.size == filtered.size)
                pos.ilctx.mkCombine(filtered, typeToSort(lvalue.sightType), lvalue.sightType)
            }
            else -> TODO("Not implemented yet")
        }
    }

    override fun writeUnsafe(lvalue: UnsafeLValue<out USort, IlType>, value: UExpr<out USort>, valueType: IlType) {
        when (val base = lvalue.base) {
            is UArrayIndexLValue<*, *, *> -> {
                val elemType = base.arrayType as IlType
                val affectedKeys = getAffectedIndices(
                    base.ref,
                    elemType,
                    base.ref.ilctx.typeToSort(elemType),
                    lvalue.offset,
                    lvalue.sightType
                )
                affectedKeys.forEach { (e, et, s, _, k) ->
                    val newValue = writeExprUnsafe(e, et, value, valueType, s)
                    write(k, newValue)
                }
            }

            is URegisterStackLValue<*> -> {
                val regType = callStack.lastMethod().typeOfRegister(base.idx)
                val oldValue = read(base)
                val newValue = writeExprUnsafe(oldValue, regType, value, valueType, lvalue.offset)
                write(base, newValue)
            }

            is UFieldLValue<*, *> -> {
                val field = base.field as IlField
                val affectedFields = getAffectedFields(field.declaringType, lvalue.offset, lvalue.sightType) { field ->
                    read(UFieldLValue(lvalue.offset.ilctx.typeToSort(field.fieldType), base.ref, field))
                }
                affectedFields.forEach { (fv, ft, s, _, _, f) ->
                    val newValue = writeExprUnsafe(fv, ft, value, valueType, s)
                    val key = UFieldLValue(lvalue.offset.ilctx.typeToSort(f.fieldType), base.ref, f)
                    write(key, newValue)
                }
            }

            else -> TODO("Not implemented yet")
        }
    }

    private fun commonReadFields(
        type: IlType,
        offset: UExpr<UBvSort>,
        pos: UExpr<UBvSort>,
        viewType: IlType,
        readField: (IlField) -> UExpr<out USort>
    ) : List<UExpr<out USort>> {
        val fields = getAffectedFields(type, offset, viewType, readField)
        return fields.flatMap { (v, vt, s, e, fieldOffset) ->
            val p = offset.ctx.mkBvAddExpr(pos, fieldOffset)
            readExprUnsafe(v, vt, viewType, s, e, p, posIsStable = false)
        }
    }

    private fun writeStructUnsafe(struct: IlStruct, offset: UExpr<UBvSort>, valueType: IlType, value: UExpr<out USort>) : UExpr<out USort> {
        val affectedFields = commonWriteFields(struct.type, offset, valueType, value) { f -> struct.fields[f]!! }
        return affectedFields.fold(struct) { acc, (f, value) ->
            acc.writeField(f, value, ownership)
        }
    }

    private fun commonWriteFields(
        type: IlType,
        offset: UExpr<UBvSort>,
        viewType: IlType,
        value: UExpr<out USort>,
        readField: (IlField) -> UExpr<out USort>
    ) : List<Pair<IlField, UExpr<out USort>>> {
        val affectedFields = getAffectedFields(type, offset, viewType, readField)
        return affectedFields.map { (fv, ft, s, _, _, f) ->
            val newValue = writeExprUnsafe(fv, ft, value, viewType, s)
            f to newValue
        }
    }

    // TODO possible index out of bounds because of extra + 1
    private fun <Sort : USort> getAffectedIndices(
        arrayRef: UHeapRef,
        elementType: IlType,
        elemSort: Sort,
        offset: UExpr<UBvSort>,
        sightType: IlType
    ): List<IlAffectedIndex<Sort>> {
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
                val value = read(key)
                val start = mkBvSubExpr(offset, currentOffset)
                val end = mkBvAddExpr(start, viewSizeBv)
                currentOffset = mkBvAddExpr(currentOffset, elemSizeBv)
                IlAffectedIndex(value, elementType, start, end, key.cast())
            }
        }
    }

    // TODO optimize if start (so the end is) are concrete
    private fun getAffectedFields(
        type: IlType,
        offset: UExpr<UBvSort>,
        viewType: IlType,
        readField: (IlField) -> UExpr<out USort>
    ): List<IlAffectedField<out USort>> {
        val end = with(offset.ctx) {
            mkBvAddExpr(offset, mkBv(viewType.size, bv32Sort))
        }
        val fieldTypeSize = type.size
        val fields = type.fields.sortedBy { it.offset }
        val fieldsWithZeros = LinkedList<IlField>()
        fields.foldRight(fieldTypeSize) { field, nextOffset ->
            val fieldOffset = field.offset
            val size = field.fieldType.size
            val extraZerosCount = max(0, nextOffset - fieldOffset - size)
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
                val value = readField(it)
                val fieldOffset : UExpr<UBvSort> = mkBv(it.offset, bv32Sort)
                val affectedStart = mkBvSubExpr(offset, fieldOffset)
                val affectedEnd = mkBvSubExpr(end, fieldOffset)
                IlAffectedField(value, it.fieldType, affectedStart, affectedEnd, fieldOffset, it)
            }
        }
    }

    private fun <Sort: USort> readExprUnsafe(
        expr: UExpr<Sort>,
        exprType: IlType,
        sightType: IlType,
        start: UExpr<UBvSort>,
        end: UExpr<UBvSort>,
        pos: UExpr<UBvSort>,
        posIsStable: Boolean
    ): List<UExpr<out USort>> {
        return when (expr) {
            is IlStruct -> {
                commonReadFields(exprType, start, pos, sightType) { f ->
                    expr.fields[f]!!
                }
            }

            is Slice<Sort> -> {
                val cut = Cut(start, end, pos, posIsStable)
                val newExpr = expr.ilctx.addCut(expr, cut)
                listOf(newExpr)
            }

            is Combine<out USort> -> {
                val slices = expr.slices
                slices.flatMap { readExprUnsafe(it.expr, exprType, sightType, start, end, pos, posIsStable) }
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
    private fun <Sort : USort> writeExprUnsafe(
        expr: UExpr<Sort>,
        exprType: IlType,
        value: UExpr<out USort>,
        valueType: IlType,
        start: UExpr<UBvSort>,
    ): UExpr<out USort> = with(expr.ilctx) {
        when {
            start == mkBv(0, bv32Sort) && valueType.size == exprType.size -> value
            expr.sort == addressSort -> TODO()
            expr.sort is StructSort -> writeStructUnsafe(expr.toStruct(), start, valueType, value)
            else -> {
                val exprSize: UExpr<UBvSort> = mkBv(exprType.size, bv32Sort)
                val valueSize: UExpr<UBvSort> = mkBv(valueType.size, bv32Sort)
                val zero: UExpr<UBvSort> = mkBv(0, bv32Sort)
                val leftUnaffected = readExprUnsafe(expr, exprType, valueType, zero, start, zero, posIsStable = true)
                val rightUnaffectedStart = mkBvAddExpr(start, valueSize)
                val rightUnaffected =
                    readExprUnsafe(expr, exprType, valueType, rightUnaffectedStart, exprSize, rightUnaffectedStart, posIsStable = true)
                val valueSlices = readExprUnsafe(
                    value,
                    valueType,
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
        }
    }

    override fun clone(
        typeConstraints: UTypeConstraints<IlType>,
        thisOwnership: MutabilityOwnership,
        cloneOwnership: MutabilityOwnership
    ): IlMemory =
        IlMemory(ctx, cloneOwnership, typeConstraints, callStack, stack.clone(), mocks.clone(), regions).also {
            it.ownership = thisOwnership
        }
}

private data class IlAffectedIndex<Sort: USort>(
    override val value: UExpr<Sort>,
    override val valueType: IlType,
    override val start: UExpr<UBvSort>,
    override val end: UExpr<UBvSort>,
    val key: UArrayIndexLValue<IlType, Sort, USizeSort>
) : AffectedValue<IlType, Sort>

private data class IlAffectedField<Sort: USort>(
    override val value: UExpr<Sort>,
    override val valueType: IlType,
    override val start: UExpr<UBvSort>,
    override val end: UExpr<UBvSort>,
    val fieldOffset: UExpr<UBvSort>,
    val field: IlField
) : AffectedValue<IlType, Sort>


private val zeroField : IlField
    get() = TODO()
