package org.usvm.org.usvm.expressions

import io.ksmt.expr.KExpr
import org.jacodb.api.common.CommonType
import org.usvm.UBvSort
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.expressions.Combine
import org.usvm.expressions.Cut
import org.usvm.expressions.Slice
import org.usvm.expressions.size
import org.usvm.solver.UExprTranslator

open class UnsafeTranslator<Type, USizeSort : USort>(override val ctx: UContext<USizeSort>) :
    UExprTranslator<Type, USizeSort>(ctx),
    UnsafeTransformer<Type, USizeSort> {
    override fun <Sort : USort> transform(slice: Slice<Sort>): Slice<Sort> {
        val expr = translate(slice.expr)
        require(expr.sort is UBvSort) { "transformSlice: expr is expected to have bitvector sort after translation, but was ${expr.sort}" }
        val cuts = slice.cuts.mapToLinkedList {
            Cut(
                translate(it.start),
                translate(it.end),
                translate(it.pos),
                it.posIsStable
            )
        }
        return ctx.mkSlice(expr, cuts)
    }

    private fun <Sort: UBvSort> bvMax(lhs: UExpr<Sort>, rhs: UExpr<Sort>): KExpr<Sort> {
        return with (lhs.ctx) {
            mkIte(mkBvSignedLessExpr(lhs, rhs),
                rhs,
                lhs)
        }
    }

    private fun <Sort: UBvSort> bvMin(lhs: UExpr<Sort>, rhs: UExpr<Sort>): KExpr<Sort> {
        return with (lhs.ctx) {
            mkIte(
                mkBvSignedLessExpr(lhs, rhs),
                lhs,
                rhs
            )
        }
    }

    // TODO pass size of combine
    private fun <Sort: UBvSort> computeSliceBounds(slice: Slice<Sort>): Triple<UExpr<UBvSort>, UExpr<UBvSort>, UExpr<UBvSort>> {
        val exprSize = slice.expr.sort.sizeBits
        return with(slice.expr.ctx) {
            val zero = mkBv(0, exprSize)
            val size = mkBv(exprSize.toInt(), exprSize)
            slice.cuts.foldRight(
                Triple<UExpr<UBvSort>, UExpr<UBvSort>, UExpr<UBvSort>>(
                    zero,
                    size,
                    zero
                )
            ) { cut, (accS, accE, accP) ->
                val s = cut.start
                val e = cut.end
                val p = cut.pos
                var cutLeft = mkBvSubExpr(s, accP)
                cutLeft = bvMax(zero, cutLeft)
                var right = mkBvSubExpr(e, accE)
                right = bvMin(right, size)
                val sliceSize = mkBvSubExpr(right, cutLeft)
                val newS = mkBvAddExpr(accS, cutLeft)
                val newE = mkBvAddExpr(newS, sliceSize)
                val newPos = if (cut.posIsStable) {
                    bvMax(accP, p)
                } else {
                    bvMax(mkBvAddExpr(accP, p), zero)
                }
                Triple(newS, newE, newPos)
            }
        }
    }

    // TODO check if intersects condition and ite will be more efficient
    @Suppress("UNCHECKED_CAST")
    override fun <Sort : USort> transform(combine: Combine<Sort>): KExpr<Sort> = with(combine.ctx) {
        val sightTypeSize = combine.sightType.size
        val zero : UExpr<UBvSort> = mkBv(0, sightTypeSize.toUInt())
        val resSizeBits = zero.sort.sizeBits
        val result = combine.slices.fold(zero) { acc, slice ->
            val translatedSlice = this@UnsafeTranslator.transform(slice)
            translatedSlice as Slice<UBvSort>
            val (s, e, p) = computeSliceBounds(translatedSlice)
            val bitsMulti : KExpr<UBvSort> = mkBv(8, translatedSlice.expr.sort)
            val sBit = mkBvMulExpr(s, bitsMulti)
            val eBit = mkBvMulExpr(e, bitsMulti)
            val pBit = mkBvMulExpr(p, bitsMulti)
            val exprSizeBits = translatedSlice.expr.sort.sizeBits
            val exprSize = mkBv(exprSizeBits.toInt(), exprSizeBits)
            val cutRight = mkBvSubExpr(exprSize, eBit)
            var res = mkBvShiftLeftExpr(acc, cutRight)
            val cutLeft = mkBvSubExpr(exprSize, sBit)
            res = mkBvLogicalShiftRightExpr(res, mkBvAddExpr(cutRight, cutLeft))
            res = if (exprSizeBits > sightTypeSize.toUInt())
                mkBvExtractExpr(high = sightTypeSize - 1, low = 0, value = res)
            else {
                val diff = sightTypeSize.toUInt() - exprSizeBits
                mkBvZeroExtensionExpr(diff.toInt(), res)
            }
            val shifted = mkBvShiftLeftExpr(res, pBit)
            mkBvOrExpr(acc, shifted)
        }
        return result as KExpr<Sort>
    }
}
