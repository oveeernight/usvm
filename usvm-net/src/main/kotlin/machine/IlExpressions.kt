package org.usvm.machine

import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.*
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import io.ksmt.sort.KSortVisitor
import org.usvm.UBoolExpr
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.isTrue

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

val UExpr<out USort>.ilctx get() = ctx as IlContext

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
