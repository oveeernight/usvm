package org.usvm.machine.interpreter

import io.ksmt.sort.KFpSort
import io.ksmt.utils.asExpr
import org.jacodb.api.net.ilinstances.*
import org.usvm.*
import org.usvm.machine.IlContext
import org.usvm.machine.USizeSort
import org.usvm.machine.ilctx
import org.usvm.machine.interpreter.IlBinaryOperator.Xor.toBvIte


@Suppress("UNUSED_PARAMETER")
sealed class IlBinaryOperator(
    val onBv: IlContext.(UExpr<UBvSort>, UExpr<UBvSort>) -> UExpr<out USort> = shouldNotBeCalled,
    val onFp: IlContext.(UExpr<KFpSort>, UExpr<KFpSort>) -> UExpr<out USort> = shouldNotBeCalled,
    val onBool: IlContext.(UExpr<UBoolSort>, UExpr<UBoolSort>) -> UExpr<out USort> = shouldNotBeCalled
) {

    object Add : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvAddExpr,
        onFp = { a, b -> mkFpAddExpr(fpRoundingModeSortDefaultValue(), a, b) }
    )

    object Sub : IlBinaryOperator(
        onBv = UContext<USizeSort>::mkBvSubExpr,
        onFp = { a, b -> mkFpSubExpr(fpRoundingModeSortDefaultValue(), a, b) }
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
        onBv = { a, b -> mkEq(a, b).toBvIte() },
        onFp =  {a, b -> mkFpEqualExpr(a, b).toBvIte() },
        onBool = { a, b -> mkEq(a, b).toBvIte() }
    )

    object CNe : IlBinaryOperator(
        onBv = { a, b -> a.neq(b).toBvIte() },
        onFp = { a, b -> mkFpEqualExpr(a, b).not().toBvIte() },
        onBool = { a, b -> a.neq(b).toBvIte() }
    )

    object CGe : IlBinaryOperator(
        onBv = { a, b -> mkBvSignedGreaterOrEqualExpr(a, b).toBvIte() },
        onFp = { a, b ->
            mkIte(
                mkOr(mkFpIsNaNExpr(a), mkFpIsNaNExpr(b)),
                mkBv(0, bv32Sort),
                mkIte(
                    mkFpGreaterOrEqualExpr(a, b),
                    mkBv(1, bv32Sort),
                    mkBv(0, bv32Sort),
                )
            )
        },
    )

    object CGt : IlBinaryOperator(
        onBv = { a, b -> mkBvSignedGreaterExpr(a, b).toBvIte() },
        onFp = { a, b ->
            mkIte(
                mkOr(mkFpIsNaNExpr(a), mkFpIsNaNExpr(b)),
                mkBv(0, bv32Sort),
                mkIte(
                    mkFpGreaterExpr(a, b),
                    mkBv(1, bv32Sort),
                    mkBv(0, bv32Sort),
                )
            )
        }
    )

    object CLe : IlBinaryOperator(
        onBv = { a, b -> mkBvSignedLessOrEqualExpr(a, b).toBvIte() } ,
        onFp = { a, b ->
            mkIte(
                mkOr(mkFpIsNaNExpr(a), mkFpIsNaNExpr(b)),
                mkBv(0, bv32Sort),
                mkIte(
                    mkFpLessOrEqualExpr(a, b),
                    mkBv(1, bv32Sort),
                    mkBv(0, bv32Sort),
                )
            )
        }
    )
    object CLt : IlBinaryOperator(
        onBv = { a, b -> mkBvSignedLessExpr(a, b).toBvIte() },
        onFp = { a, b ->
            mkIte(
                mkOr(mkFpIsNaNExpr(a), mkFpIsNaNExpr(b)),
                mkBv(0, bv32Sort),
                mkIte(
                    mkFpLessExpr(a, b),
                    mkBv(1, bv32Sort),
                    mkBv(0, bv32Sort),
                )
            )
        }
    )

    object Shl : IlBinaryOperator(
        onBv = { arg, shift -> mkBvShiftLeftExpr(arg, normalizeBvShift(shift))}
    )

    object Shr : IlBinaryOperator(
        onBv = { arg, shift -> mkBvArithShiftRightExpr(arg, normalizeBvShift(shift))}
    )

    object UShr : IlBinaryOperator(
        onBv = { arg, shift -> mkBvLogicalShiftRightExpr(arg, normalizeBvShift(shift))}
    )

    internal operator fun invoke(lhs: UExpr<out USort>, rhs: UExpr<out USort>): UExpr<out USort> {
        assert(lhs.sort == rhs.sort)
        val ctx = lhs.ilctx
        return when (val sort = lhs.sort) {
            is UBvSort -> {
                ctx.onBv(lhs.asExpr(sort), rhs.asExpr(sort))
            }

            is UFpSort -> {
                ctx.onFp(lhs.asExpr(sort), rhs.asExpr(sort))
            }

            is UBoolSort -> {
                ctx.onBool(lhs.asExpr(sort), rhs.asExpr(sort)) }
            else -> error("IlBinaryOperator: unexpected sorts: $sort")
        }
    }

    protected fun UExpr<UBoolSort>.toBvIte() : UExpr<UBvSort> =
        ctx.mkIte(this,
            ctx.mkBv(1, ctx.bv32Sort),
            ctx.mkBv(0, ctx.bv32Sort)
        )

    companion object {
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
        private val shouldNotBeCalled: IlContext.(UExpr<out USort>, UExpr<out USort>) -> UExpr<out USort> =
            { _, _ -> error("Should not be called") }

        /**
         * Normalize binary shift value according to the specification.
         * */
        internal fun <T : UBvSort> normalizeBvShift(shift: UExpr<T>): UExpr<T> = with(shift.uctx) {
            return when (shift.sort) {
                bv32Sort -> {
                    val mask = mkBv(31) // 0b11111
                    mkBvAndExpr(shift.asExpr(bv32Sort), mask).asExpr(shift.sort)
                }

                bv64Sort -> {
                    val mask = mkBv(63L) // 0b111111
                    mkBvAndExpr(shift.asExpr(bv64Sort), mask).asExpr(shift.sort)
                }

                else -> error("Incorrect bv shift: $shift")
            }
        }
    }
}
