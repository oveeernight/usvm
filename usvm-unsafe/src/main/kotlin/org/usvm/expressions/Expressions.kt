package org.usvm.expressions

import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.KExpr
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import org.jacodb.api.common.CommonType
import org.usvm.*
import org.usvm.org.usvm.expressions.UnsafeTransformer
import java.util.*

class Cut(
    val start: UExpr<UBvSort>,
    val end: UExpr<UBvSort>,
    val pos: UExpr<UBvSort>,
    val posIsStable: Boolean
)

class Slice<Sort : USort>(
    ctx: UContext<*>,
    val expr: UExpr<Sort>,
    val cuts: LinkedList<Cut>,
) : UExpr<Sort>(ctx) {
    override val sort: Sort
        get() = TODO("Not yet implemented")

    override fun accept(transformer: KTransformerBase): KExpr<Sort> {
        require(transformer is UnsafeTransformer<*, *>) { "Expected a UTransformer, but got: $transformer" }
        return transformer.transform(this)
    }

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        TODO("Not yet implemented")
    }
}

class Combine<Sort: USort>(
    ctx: UContext<*>,
    val slices: List<Slice<Sort>>,
    val sightType: CommonType
): UExpr<Sort>(ctx){
    override val sort: Sort
        get() = TODO("Not yet implemented")

    override fun accept(transformer: KTransformerBase): KExpr<Sort> {
        require(transformer is UnsafeTransformer<*, *>) { "Expected a UTransformer, but got: $transformer" }
        return transformer.transform(this)
    }

    override fun internEquals(other: Any): Boolean = structurallyEqual(other)

    override fun internHashCode(): Int = hash()

    override fun print(printer: ExpressionPrinter) {
        TODO("Not yet implemented")
    }

}
