package org.usvm.expressions

import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.KExpr
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UBvSort
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.org.usvm.expressions.UnsafeTransformer
import java.util.LinkedList

data class Cut(
    val start: UExpr<UBvSort>,
    val end: UExpr<UBvSort>,
    val pos: UExpr<UBvSort>,
    val posIsStable: Boolean
) {
    fun print(printer: ExpressionPrinter) {
        start.print(printer)
        printer.append("..")
        end.print(printer)
        printer.append(" at ")
        pos.print(printer)
    }
}

class Slice<Sort : USort> internal constructor(
    ctx: UContext<*>,
    val expr: UExpr<Sort>,
    val exprType: IlType,
    val cuts: LinkedList<Cut>,
) : UExpr<Sort>(ctx) {
    override val sort: Sort
        get() = TODO("Not yet implemented")

    override fun accept(transformer: KTransformerBase): KExpr<Sort> {
        require(transformer is UnsafeTransformer<*, *>) { "Expected an UTransformer, but got: $transformer" }
        return transformer.transform(this)
    }

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        expr.print(printer)
        printer.append("[")
        cuts.forEach {
            it.print(printer)
            printer.append(" ")
        }
        printer.append("]")
    }
}

class Combine<Sort: USort>(
    ctx: UContext<*>,
    val slices: List<Slice<out USort>>,
    override val sort: Sort,
    val sightType: IlType
): UExpr<Sort>(ctx){

    override fun accept(transformer: KTransformerBase): KExpr<Sort> {
        require(transformer is UnsafeTransformer<*, *>) { "Expected an UnsafeTransformer, but got: $transformer" }
        return transformer.transform(this)
    }

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        printer.append("[")
        slices.forEach {
            it.print(printer)
            printer.append("\n")
        }
        printer.append("] as ${sightType.typeName}")
    }
}
