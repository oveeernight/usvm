package org.usvm.machine

import io.ksmt.KAst
import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.*
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import io.ksmt.sort.KSortVisitor
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.state.IlHeapLocation
import org.usvm.machine.state.IlLocation
import org.usvm.machine.state.IlStackLocation
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

class VoidValue(ctx: IlContext) : UExpr<USort>(ctx) {
    override val sort: USort = VoidSort(ctx)

    override fun accept(transformer: KTransformerBase): VoidValue = this

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        printer.append("void")
    }
}

class IlManagedRef<Key, Sort : USort>(ctx: IlContext, val memoryKey: ULValue<Key, Sort>) : UExpr<UAddressSort>(ctx) {
    override val sort: UAddressSort get() = uctx.addressSort
    override fun accept(transformer: KTransformerBase): KExpr<UAddressSort> {
        require(transformer is IlTransformer) { "Expected an IlTransformer, but got: $transformer" }
        return transformer.transform(this)
    }
    @Suppress("UNCHECKED_CAST")
    fun toBaseAndOffset() : Pair<IlLocation<*>, UExpr<USizeSort>> =
        with(sort.ilctx) {
            when (memoryKey) {
                is UArrayIndexLValue<*, *, *> -> {
                    val elemType = memoryKey.arrayType as IlType
                    val base = IlHeapLocation(memoryKey.ref, memoryKey.sort, elemType, isArray = true)
                    val elemSize = mkSizeExpr(elemType.size)
                    val idx = memoryKey.index as UExpr<USizeSort>
                    val offset = mkBvMulExpr(idx, elemSize)
                    base to offset
                }
                is UFieldLValue<*, *> -> {
                    val field = memoryKey.field as IlField
                    val declaringType = field.declaringType
                    val base = IlHeapLocation(memoryKey.ref, sort, declaringType, isArray = false)
                    val offset = mkSizeExpr(field.offset)
                    base to offset
                }

                is URegisterStackLValue<*> -> {
                    val base = IlStackLocation(memoryKey)
                    base to mkSizeExpr(0)
                }
                else -> error("Unsupported memory key: $memoryKey")
            }
        }

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        TODO("Not yet implemented")
    }
}


class IlPtr<Sort: USort>(
    ctx: UContext<*>,
    override val location: IlLocation<Sort>,
    override val offset: UExpr<UBvSort>,
    override val sightType: IlType
): UExpr<UAddressSort>(ctx), UnsafeLValue<Sort> {
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
