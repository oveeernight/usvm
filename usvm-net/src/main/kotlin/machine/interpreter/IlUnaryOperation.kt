package org.usvm.machine.interpreter

import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlExpr
import org.jacodb.api.net.ilinstances.IlNegOp
import org.jacodb.api.net.ilinstances.IlNotOp
import org.jacodb.api.net.ilinstances.IlUnaryOp
import org.usvm.UBoolSort
import org.usvm.UBvSort
import org.usvm.UExpr
import org.usvm.UFpSort
import org.usvm.USort
import org.usvm.machine.IlContext
import org.usvm.machine.ilctx

sealed class IlUnaryOperator(
    private val onBv : IlContext.(UExpr<UBvSort>) -> UExpr<out USort> = shouldNotBeCalled,
    private val onFp : IlContext.(UExpr<UFpSort>) -> UExpr<out USort> = shouldNotBeCalled,
    private val onBool : IlContext.(UExpr<UBoolSort>) -> UExpr<out USort> = shouldNotBeCalled
) {
    object Not : IlUnaryOperator(
        onBool = { a -> mkNot(a) }
    )

    object Neg : IlUnaryOperator(
        onBv = IlContext::mkBvNegationExpr,
        onFp = IlContext::mkFpNegationExpr
    )

    operator fun invoke(expr: UExpr<out USort>) : UExpr<out USort> =
        when (expr.sort) {
            is UBvSort -> expr.ilctx.onBv(expr.cast())
            is UFpSort -> expr.ilctx.onFp(expr.cast())
            is UBoolSort -> expr.ilctx.onBool(expr.cast())
            else -> error("IlUnaryOperator: unexpected sort sort ${expr.sort}")
        }

    companion object {
        fun resolve(op: IlUnaryOp) : IlUnaryOperator {
            return when (op) {
                is IlNegOp -> Neg
                is IlNotOp -> Not
                else -> error("IlUnaryOperator: unexpected op ${op}")
            }
        }
        val shouldNotBeCalled: IlContext.(UExpr<out USort>) -> UExpr<out USort> = { _ -> error("Should not be called") }
    }
}
