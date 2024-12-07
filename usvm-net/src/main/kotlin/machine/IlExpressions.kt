package org.usvm.machine

import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.KExpr
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import io.ksmt.sort.KSortVisitor
import org.usvm.UExpr
import org.usvm.USort

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
