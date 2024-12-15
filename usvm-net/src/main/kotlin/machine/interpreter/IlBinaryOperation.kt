package org.usvm.machine.interpreter

import io.ksmt.sort.KBvSort
import io.ksmt.sort.KFpSort
import io.ksmt.utils.asExpr
import org.jacodb.api.net.ilinstances.IlBinaryOp
import org.jacodb.api.net.ilinstances.IlCeqOp
import org.usvm.UBoolSort
import org.usvm.UBvSort
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.machine.IlContext
import org.usvm.machine.ilctx

@Suppress("UNUSED_PARAMETER")
sealed class IlBinaryOperation(
    val onBv: IlContext.(UExpr<UBvSort>, UExpr<UBvSort>) -> UExpr<out USort> = error("Should not be called"),
    val onFp: IlContext.(UExpr<KFpSort>, UExpr<KFpSort>) -> UExpr<out USort> = error("Should not be called"),
    val onBool: IlContext.(UExpr<UBoolSort>, UExpr<UBoolSort>) -> UExpr< out USort> = error("Should not be called")) {

    object CEqExpr: IlBinaryOperation(
        onBv = { a, b -> mkEq(a, b) },
        onFp = { a, b -> mkFpEqualExpr(a, b) },
        onBool =   {a, b ->  mkEq(a, b) },
    )

    internal operator fun invoke(lhs: UExpr<out USort>, rhs: UExpr<out USort>): UExpr<out USort> {
        assert(lhs.sort == rhs.sort)
        val ctx = lhs.ilctx
        return when (lhs.sort) {
            is UBvSort -> {
                lhs.ilctx.onBv(lhs.asExpr(ctx.bv32Sort), rhs.asExpr(ctx.bv32Sort))
            }
            else -> TODO()
        }
    }

    companion object {
        fun resolve(op: IlBinaryOp) :IlBinaryOperation {
            return when (op) {
                 is IlCeqOp -> CEqExpr
                else -> TODO()
            }
        }
    }
}
