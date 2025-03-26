package org.usvm.org.usvm.expressions

import io.ksmt.expr.KExpr
import org.usvm.UBvSort
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.expressions.*
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
        return ctx.mkSlice(expr, slice.exprType, cuts)
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
        val exprBitSize = (slice.exprType.size * 8).toUInt()
        return with(slice.expr.ctx) {
            val zero = mkBv(0, exprBitSize)
            val byteSize = mkBv(slice.exprType.size, exprBitSize)
            slice.cuts.foldRight(
                Triple<UExpr<UBvSort>, UExpr<UBvSort>, UExpr<UBvSort>>(
                    zero,
                    byteSize,
                    zero
                )
            ) { cut, (accS, accE, accP) ->
                val s = cut.start
                val e = cut.end
                val p = cut.pos
                var cutLeft = mkBvSubExpr(s, accP)
                cutLeft = bvMax(cutLeft, zero)
                var right = mkBvSubExpr(e, accP)
                right = bvMin(right, accE)
                val sliceSize = mkBvSubExpr(right, cutLeft)
                val newS = mkBvAddExpr(accS, cutLeft)
                val newE = bvMin(mkBvAddExpr(newS, sliceSize), accE)
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
        val sightTypeBitSize = combine.sightType.size * 8
        val zero : UExpr<UBvSort> = mkBv(0, sightTypeBitSize.toUInt())
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
            var res = mkBvShiftLeftExpr(translatedSlice.expr, cutRight)
            res = mkBvLogicalShiftRightExpr(res, mkBvAddExpr(cutRight, sBit))
            res = if (exprSizeBits > sightTypeBitSize.toUInt())
                mkBvExtractExpr(high = sightTypeBitSize - 1, low = 0, value = res)
            else {
                val diff = sightTypeBitSize.toUInt() - exprSizeBits
                mkBvZeroExtensionExpr(diff.toInt(), res)
            }
            val shifted = mkBvShiftLeftExpr(res, pBit)
            mkBvOrExpr(acc, shifted)
        }
        return result as KExpr<Sort>
    }
}
