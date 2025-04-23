package org.usvm.machine

import com.jetbrains.rd.util.string.print
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
import org.usvm.machine.state.IlRegisterStackLValue
import org.usvm.memory.ULValue
import org.usvm.memory.UMemoryRegion
import org.usvm.memory.UnsafeLValue

class VoidSort(ctx: IlContext) : USort(ctx) {
    override fun <T> accept(visitor: KSortVisitor<T>): T {
        error("Should not be called")
    }
    override fun print(builder: StringBuilder) {
        builder.append("void sort")
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

class IlManagedRef<Sort : USort>(
    ctx: IlContext,
    val type: IlType,
    val memoryKey: ULValue<*, Sort>
) : UExpr<UAddressSort>(ctx) {
    override val sort: UAddressSort
        get() = uctx.addressSort
    override fun accept(transformer: KTransformerBase): KExpr<UAddressSort> {
        require(transformer is IlTransformer) { "Expected an IlTransformer, but got: $transformer" }
        return transformer.transform(this)
    }
    @Suppress("UNCHECKED_CAST")
    fun toBaseAndOffset() : Pair<ULValue<*, Sort>, UExpr<USizeSort>> =
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

                is IlRegisterStackLValue<*> -> {
                    val offset : UExpr<USizeSort> = mkSizeExpr((0))
                    memoryKey to offset
                }

                else -> error("Unsupported memory key: $memoryKey")
            }
        }.cast()

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        printer.append("&${memoryKey}")
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
