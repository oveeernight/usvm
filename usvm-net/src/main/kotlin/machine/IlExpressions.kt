package org.usvm.machine

import io.ksmt.KAst
import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.*
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import io.ksmt.sort.KSortVisitor
import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.machine.state.IlMemory
import org.usvm.memory.ULValue
import org.usvm.memory.URegisterStackLValue
import org.usvm.memory.UnsafeLValue

class VoidSort(ctx: IlContext) : USort(ctx) {
    override fun <T> accept(visitor: KSortVisitor<T>): T {
        error("Should not be called")
    }
    override fun print(builder: StringBuilder) {
        builder.append("void sort")
    }
}

class StructSort(ctx: IlContext) : USort(ctx) {
    override fun <T> accept(visitor: KSortVisitor<T>): T {
        error("Should not be called")
    }
    override fun print(builder: StringBuilder) {
        builder.append("struct sort")
    }
}

class VoidValue(ctx: IlContext) : UExpr<USort>(ctx) {
    override val sort: USort = ctx.voidSort

    override fun accept(transformer: KTransformerBase): VoidValue = this

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        printer.append("void")
    }
}

class IlStruct(ctx: IlContext, val type: IlType, val fields: UPersistentHashMap<IlField, UExpr<out USort>>): UExpr<StructSort>(ctx) {
    override val sort: StructSort = ctx.structSort

    fun writeField(field: IlField, value: UExpr<out USort>, ownership: MutabilityOwnership) : IlStruct {
        val updatedFields = fields.put(field, value, ownership)
        return value.ilctx.mkStruct(type, updatedFields)
    }

    override fun accept(transformer: KTransformerBase): KExpr<StructSort> {
        TODO("Not yet implemented")
    }

    override fun internEquals(other: Any): Boolean {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int {
        TODO("Not yet implemented")
    }

    override fun print(printer: ExpressionPrinter) {
        TODO("Not yet implemented")
    }

}

abstract class IlManagedRef<Key, Sort: USort>(ctx: IlContext) : UExpr<UAddressSort>(ctx) {
    override val sort: UAddressSort
        get() = uctx.addressSort
    abstract val type: IlType
    abstract val memoryKey: Key
    abstract fun toBaseAndOffset() : Pair<Key, UExpr<USizeSort>>
    abstract fun read(memory: IlMemory) : UExpr<Sort>
    abstract fun write(memory: IlMemory, value: UExpr<out USort>)
}

class IlManagedHeapRef<Sort : USort>(
    ctx: IlContext,
    override val type: IlType,
    override val memoryKey: ULValue<*, Sort>
) : IlManagedRef<ULValue<*, Sort>, Sort>(ctx) {
    override fun accept(transformer: KTransformerBase): KExpr<UAddressSort> {
        require(transformer is IlTransformer) { "Expected an IlTransformer, but got: $transformer" }
        return transformer.transform(this)
    }
    @Suppress("UNCHECKED_CAST")
    override fun toBaseAndOffset() : Pair<ULValue<*, Sort>, UExpr<USizeSort>> =
        with(sort.ilctx) {
            when (memoryKey) {
                is UArrayIndexLValue<*, *, *> -> {
                    val elemType = memoryKey.arrayType as IlType
                    val elemSize = mkSizeExpr(elemType.size)
                    val idx = memoryKey.index as UExpr<USizeSort>
                    val offset : UExpr<USizeSort> = mkBvMulExpr(idx, elemSize)
                    memoryKey to offset
                }
                is UFieldLValue<*, *> -> {
                    val field = memoryKey.field as IlField
                    val offset : UExpr<USizeSort> = mkSizeExpr(field.offset)
                    memoryKey to offset
                }

                else -> error("Unsupported memory key: $memoryKey")
            }
        }.cast()

    override fun read(memory: IlMemory): UExpr<Sort> = memory.read(memoryKey)

    override fun write(memory: IlMemory, value: UExpr<out USort>) = memory.write(memoryKey, value)

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        TODO("Not yet implemented")
    }
}

class IlManagedStackRef<Sort : USort>(
    ctx: IlContext,
    override val type: IlType,
    override val memoryKey: URegisterStackLValue<Sort>,
    private val frameIdx: Int
) : IlManagedRef<URegisterStackLValue<Sort>, Sort>(ctx) {
    override val sort: UAddressSort
        get() = uctx.addressSort

    override fun accept(transformer: KTransformerBase): KExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        TODO("Not yet implemented")
    }

    override fun toBaseAndOffset(): Pair<URegisterStackLValue<Sort>, UExpr<USizeSort>> {
        val offset = ilctx.mkSizeExpr(0)
        return (memoryKey to offset)
    }

    override fun read(memory: IlMemory): UExpr<Sort> {
        return memory.stack.readFrame(frameIdx, memoryKey.idx, memoryKey.sort)
    }

    override fun write(memory: IlMemory, value: UExpr<out USort>) {
        memory.stack.writeFrame(frameIdx, memoryKey.idx, value)
    }

}


class IlPtr<Sort: USort>(
    ctx: UContext<*>,
    override val base: ULValue<*, Sort>,
    override val offset: UExpr<UBvSort>,
    override val sightType: IlType
): UExpr<UAddressSort>(ctx), UnsafeLValue<Sort, IlType> {
    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override val sort: UAddressSort
        get() = uctx.addressSort

    override fun accept(transformer: KTransformerBase): KExpr<UAddressSort> {
        require(transformer is IlTransformer) { "Expected an IlTransformer, but got: $transformer" }
        return transformer.transform(this)
    }

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        printer.append("(${sightType.name}*)")
    }
}


val KAst.ilctx get() = ctx as IlContext

@Suppress("UNCHECKED_CAST")
fun <Sort: USort> UExpr<Sort>.tryBool(): Boolean? = (this as? UBoolExpr)?.isTrue
fun <Sort: USort> UExpr<Sort>.tryInt8(): Byte? = (this as? KBitVec8Value)?.byteValue
fun <Sort: USort> UExpr<Sort>.tryInt16(): Short? = (this as? KBitVec16Value)?.shortValue
fun <Sort: USort> UExpr<Sort>.tryInt32(): Int? = (this as? KBitVec32Value)?.intValue
fun <Sort: USort> UExpr<Sort>.tryInt64(): Long? = (this as? KBitVec64Value)?.longValue
fun <Sort: USort> UExpr<Sort>.tryChar(): Char? = (this as? KBitVec16Value)?.shortValue?.toInt()?.toChar()
fun <Sort: USort> UExpr<Sort>.tryFloat(): Float? = (this as? KFp32Value)?.value
fun <Sort: USort> UExpr<Sort>.tryDouble(): Double? = (this as? KFp64Value)?.value
// TODO signed
