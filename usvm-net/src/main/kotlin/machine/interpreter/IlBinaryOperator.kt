package org.usvm.machine.interpreter

import io.ksmt.sort.KFpSort
import io.ksmt.utils.asExpr
import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.*
import org.usvm.*
import org.usvm.machine.IlContext
import org.usvm.machine.IlPtr
import org.usvm.machine.USizeSort
import org.usvm.machine.ilctx


@Suppress("UNCHECKED_CAST")
sealed class IlBinaryOperator(
    val onBv: IlContext.(UExpr<UBvSort>, UExpr<UBvSort>) -> UExpr<out USort> = shouldNotBeCalled,
    val onFp: IlContext.(UExpr<KFpSort>, UExpr<KFpSort>) -> UExpr<out USort> = shouldNotBeCalled,
    val onBool: IlContext.(UExpr<UBoolSort>, UExpr<UBoolSort>) -> UExpr<out USort> = shouldNotBeCalled,
    val onAddressSort: IlContext.(UExpr<out USort>, UExpr<out USort>) -> UExpr<out USort> = shouldNotBeCalled
) {

    object Add : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvAddExpr,
        onFp = { a, b -> mkFpAddExpr(fpRoundingModeSortDefaultValue(), a, b) },
        onAddressSort = { a, b ->
            val (l, r) = normalizePtrOp(a, b)
            l as IlPtr<*>
            shiftPointer(l, r.cast())
        }
    )

    object Sub : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvSubExpr,
        onFp = { a, b -> mkFpSubExpr(fpRoundingModeSortDefaultValue(), a, b) },
        onAddressSort = { a, b ->
            val (l, r) = normalizePtrOp(a, b)
            l as IlPtr<*>
            r as UExpr<UBvSort>
            shiftPointer(l, mkBvNegationExpr(r))
        }
    )

    object Mul : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvMulExpr,
        onFp = {a, b -> mkFpMulExpr(fpRoundingModeSortDefaultValue(), a, b)}
    )

    object Div : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvSignedDivExpr,
        onFp = { a, b -> mkFpDivExpr(fpRoundingModeSortDefaultValue(), a, b) }
    )

    object Rem : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvSignedRemExpr,
    )

    object And : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvAndExpr,
        onBool = UContext<USizeSort>::mkAnd
    )

    object Or : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvOrExpr,
        onBool = UContext<USizeSort>::mkOr
    )

    object Xor : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvXorExpr,
        onBool = UContext<USizeSort>::mkXor
    )

    object CEq : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkEq,
        onFp = UContext<USizeSort>::mkFpEqualExpr,
        onBool = UContext<USizeSort>::mkEq
    )

    object CNe : IlBinaryOperator(
        onBv = { a, b -> a.neq(b) },
        onFp = { a, b -> mkFpEqualExpr(a, b).not() },
        onBool = { a, b -> a.neq(b) }
    )

    object CGe : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvSignedGreaterOrEqualExpr,
        onFp = { a, b ->
            mkIte(
                mkOr(mkFpIsNaNExpr(a), mkFpIsNaNExpr(b)),
                falseExpr,
                mkFpGreaterOrEqualExpr(a, b)
            )
        },
    )

    object CGt : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvSignedGreaterExpr,
        onFp = { a, b ->
            mkIte(
                mkOr(mkFpIsNaNExpr(a), mkFpIsNaNExpr(b)),
                falseExpr,
                mkFpGreaterExpr(a, b),
            )
        }
    )

    object CLe : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvSignedLessOrEqualExpr,
        onFp = { a, b ->
            mkIte(
                mkOr(mkFpIsNaNExpr(a), mkFpIsNaNExpr(b)),
                falseExpr,
                mkFpLessOrEqualExpr(a, b)
            )
        }
    )
    object CLt : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvSignedLessExpr,
        onFp = { a, b ->
            mkIte(
                mkOr(mkFpIsNaNExpr(a), mkFpIsNaNExpr(b)),
                falseExpr,
                mkFpLessExpr(a, b),
            )
        }
    )

    object Shl : IlBinaryOperator(
        onBv = { arg, shift -> mkBvShiftLeftExpr(arg, shift)}
    )

    object Shr : IlBinaryOperator(
        onBv = { arg, shift -> mkBvArithShiftRightExpr(arg, shift)}
    )

    object UShr : IlBinaryOperator(
        onBv = { arg, shift -> mkBvLogicalShiftRightExpr(arg, shift)}
    )

    internal operator fun invoke(lhs: UExpr<out USort>, rhs: UExpr<out USort>): UExpr<out USort> {
        var unifiedLhs = lhs
        var unifiedRhs = rhs
        if (lhs.sort != rhs.sort) {
            if (lhs.sort is UBoolSort && rhs.sort is UBvSort) {
                unifiedLhs = lhs.asExpr(lhs.ctx.boolSort).toBvIte()
                unifiedRhs = rhs
            } else if (lhs.sort is UBvSort && rhs.sort is UBoolSort) {
                unifiedLhs = lhs
                unifiedRhs = rhs.asExpr(rhs.ctx.boolSort).toBvIte()
            } else {
                unifiedLhs = lhs
                unifiedRhs = rhs
            }
        }
        val ctx = lhs.ilctx
        return when (val sort = unifiedLhs.sort) {
            is UBvSort -> {
                ctx.onBv(unifiedLhs.asExpr(sort), unifiedRhs.asExpr(sort))
            }

            is UFpSort -> {
                ctx.onFp(unifiedLhs.asExpr(sort), unifiedRhs.asExpr(sort))
            }

            is UBoolSort -> {
                ctx.onBool(unifiedLhs.asExpr(sort), unifiedRhs.asExpr(sort))
            }

            is UAddressSort -> {
                ctx.onAddressSort(unifiedLhs, unifiedRhs)
            }
            else -> error("IlBinaryOperator: unexpected sorts: $sort")
        }
    }

    companion object {
        fun UExpr<UBoolSort>.toBvIte() : UExpr<UBvSort> =
            ctx.mkIte(this,
                ctx.mkBv(1, ctx.bv32Sort),
                ctx.mkBv(0, ctx.bv32Sort)
            )

        fun resolve(op: IlBinaryOp): IlBinaryOperator {
            return when (op) {
                is IlAddOp -> Add
                is IlSubOp -> Sub
                is IlMulOp -> Mul
                is IlDivOp -> Div
                is IlRemOp -> Rem
                is IlCeqOp -> CEq
                is IlCneOp -> CNe
                is IlCgeOp -> CGe
                is IlCgtOp -> CGt
                is IlCleOp -> CLe
                is IlCltOp -> CLt
                is IlAndOp -> And
                is IlOrOp -> Or
                is IlXorOp -> Xor
                is IlShlOp -> Shl
                is IlShrOp -> Shr
                else -> TODO()
            }
        }

        private fun normalizePtrOp(lhs: UExpr<out USort>, rhs: UExpr<out USort>) =
            if (lhs.sort is UAddressSort && rhs.sort is UBvSort) {
                lhs to rhs
            } else rhs to lhs

        fun isPointerOp(op: IlBinaryOperator): Boolean =
            when (op) {
                is Add -> true
                is Sub -> true
                is Mul -> true
                is Div -> true
                is CEq -> true
                is CNe -> true
                else -> false
            }

        private val shouldNotBeCalled: IlContext.(UExpr<out USort>, UExpr<out USort>) -> UExpr<out USort> =
            { _, _ -> error("Should not be called") }
    }
}
