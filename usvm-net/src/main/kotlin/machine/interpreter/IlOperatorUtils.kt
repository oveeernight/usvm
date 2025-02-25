package org.usvm.machine.interpreter

import org.usvm.UBoolExpr
import org.usvm.UBoolSort
import org.usvm.UBvSort
import org.usvm.UExpr

internal fun UExpr<UBoolSort>.extendToBv(sizeBits: Int): UExpr<UBvSort> {
    assert(sizeBits > 0)
    val uintSizeBits = sizeBits.toUInt()
    return ctx.mkIte(
        this,
        ctx.mkBv(1, uintSizeBits),
        ctx.mkBv(0, uintSizeBits),
    )
}


/**
 * Widens or narrows a bit-vec expression to match the [sizeBits] regarding [signed] flag.
 *
 * @return the bit-vec expression of [sizeBits] size.
 */
internal fun UExpr<UBvSort>.mkNarrow(sizeBits: Int, signed: Boolean): UExpr<UBvSort> {
    val diff = sizeBits - sort.sizeBits.toInt()
    val res = if (diff > 0) {
        if (signed) {
            ctx.mkBvSignExtensionExpr(diff, this)
        } else {
            ctx.mkBvZeroExtensionExpr(diff, this)
        }
    } else {
        ctx.mkBvExtractExpr(high = sizeBits - 1, low = 0, this)
    }
    return res
}
