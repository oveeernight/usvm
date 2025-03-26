package org.usvm.machine.interpreter

import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlNegOp
import org.jacodb.api.net.ilinstances.IlNotOp
import org.jacodb.api.net.ilinstances.IlUnaryOp
import org.usvm.*
import org.usvm.machine.IlContext
import org.usvm.machine.ilctx

sealed class IlUnaryOperator(
    private val onBv : IlContext.(UExpr<UBvSort>) -> UExpr<out USort> = shouldNotBeCalled,
    private val onFp : IlContext.(UExpr<UFpSort>) -> UExpr<out USort> = shouldNotBeCalled,
    private val onBool : IlContext.(UExpr<UBoolSort>) -> UExpr<out USort> = shouldNotBeCalled,
    private val onAddress : IlContext.(UExpr<UAddressSort>) -> UExpr<out USort> = shouldNotBeCalled
) {
    object Not : IlUnaryOperator(
        onBool = { a -> mkNot(a) }
    )

    object Neg : IlUnaryOperator(
        onBv = IlContext::mkBvNegationExpr,
        onFp = IlContext::mkFpNegationExpr
    )

    object CastToBool : IlUnaryOperator(
        onBool = { it },
        onBv = { it neq mkBv(0, it.sort) }
    )
    object CastToInt8 : IlUnaryOperator(
        onBool = { it.extendToBv(Byte.SIZE_BITS) },
        onBv = { it.mkNarrow(Byte.SIZE_BITS, signed = true) },
        onAddress = { it.toNumeric().mkNarrow(Byte.SIZE_BITS, signed = true) }
    )
    object CastToUInt8 : IlUnaryOperator(
        onBool = { it.extendToBv(UByte.SIZE_BITS) },
        onBv = { it.mkNarrow(UByte.SIZE_BITS, signed = false) },
        onAddress = { it.toNumeric().mkNarrow(UByte.SIZE_BITS, signed = false) }
    )
    object CastToInt16 : IlUnaryOperator(
        onBool = { it.extendToBv(Short.SIZE_BITS) },
        onBv = { it.mkNarrow(Short.SIZE_BITS, signed = true) },
        onAddress = { it.toNumeric().mkNarrow(Short.SIZE_BITS, signed = true) }
    )
    object CastToUInt16 : IlUnaryOperator(
        onBool = { it.extendToBv(UShort.SIZE_BITS) },
        onBv = { it.mkNarrow(UShort.SIZE_BITS, signed = false) },
        onAddress = { it.toNumeric().mkNarrow(UShort.SIZE_BITS, signed = false) }
    )
    object CastToInt32 : IlUnaryOperator(
        onBool = { it.extendToBv(Int.SIZE_BITS) },
        onBv = { it.mkNarrow(Int.SIZE_BITS, signed = true) },
        onAddress = { it.toNumeric().mkNarrow(Int.SIZE_BITS, signed = true) }
    )
    object CastToUInt32 : IlUnaryOperator(
        onBool = { it.extendToBv(UInt.SIZE_BITS) },
        onBv = { it.mkNarrow(UInt.SIZE_BITS, signed = false) },
        onAddress = { it.toNumeric().mkNarrow(UInt.SIZE_BITS, signed = false) }
    )
    object CastToInt64 : IlUnaryOperator(
        onBool = { it.extendToBv(Long.SIZE_BITS) },
        onBv = { it.mkNarrow(Long.SIZE_BITS, signed = true) },
        onAddress = { it.toNumeric().mkNarrow(Long.SIZE_BITS, signed = true) }
    )
    object CastToUInt64 : IlUnaryOperator(
        onBool = { it.extendToBv(ULong.SIZE_BITS) },
        onBv = { it.mkNarrow(ULong.SIZE_BITS, signed = false) },
        onAddress = { it.toNumeric().mkNarrow(ULong.SIZE_BITS, signed = false) }
    )
    object CastToFloat : IlUnaryOperator(
        onBv = { mkBvToFpExpr(fp32Sort, fpRoundingModeSortDefaultValue(), it , signed = true) },
        onFp = { mkFpToFpExpr(fp32Sort, fpRoundingModeSortDefaultValue(), it) },
    )
    object CastToDouble : IlUnaryOperator(
        onBv = { mkBvToFpExpr(fp64Sort, fpRoundingModeSortDefaultValue(), it , signed = true) },
        onFp = { mkFpToFpExpr(fp64Sort, fpRoundingModeSortDefaultValue(), it) }
    )

    operator fun invoke(expr: UExpr<out USort>) : UExpr<out USort> =
        when (expr.sort) {
            is UBvSort -> expr.ilctx.onBv(expr.cast())
            is UFpSort -> expr.ilctx.onFp(expr.cast())
            is UBoolSort -> expr.ilctx.onBool(expr.cast())
            is UAddressSort -> expr.ilctx.onAddress(expr.cast())
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
