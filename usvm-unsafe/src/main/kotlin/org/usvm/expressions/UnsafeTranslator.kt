package org.usvm.org.usvm.expressions

import io.ksmt.expr.KExpr
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.expressions.Combine
import org.usvm.expressions.Slice
import org.usvm.solver.UExprTranslator

open class UnsafeTranslator<Type, USizeSort : USort>(ctx: UContext<USizeSort>) : UExprTranslator<Type, USizeSort>(ctx),
    UnsafeTransformer<Type, USizeSort> {
    override fun <Sort : USort> transform(slice: Slice<Sort>): KExpr<Sort> {
        TODO("Not yet implemented")
    }

    override fun <Sort : USort> transform(combine: Combine<Sort>): KExpr<Sort> {
        TODO("Not yet implemented")
    }
}
