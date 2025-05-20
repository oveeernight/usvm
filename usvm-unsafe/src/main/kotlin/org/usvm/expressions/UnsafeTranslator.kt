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
            val zero: UExpr<UBvSort> = mkBv(0, bv32Sort)
            val byteSize: UExpr<UBvSort> = mkBv(slice.exprType.size, bv32Sort)
            slice.cuts.fold(
                Triple(
                    zero,
                    byteSize,
                    zero
                )
            ) { (accS, accE, accP), cut ->
                val s = cut.start
                val e = cut.end
                val p = cut.pos
                val cutLeft = bvMax(mkBvSubExpr(s, accP), zero)
                val right = bvMin(mkBvSubExpr(e, accP), accE)
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
            val bitsMulti : KExpr<UBvSort> = mkBv(8, bv32Sort)
            val sBit = mkBvMulExpr(s, bitsMulti)
            val eBit = mkBvMulExpr(e, bitsMulti)
            val pBit = mkBvMulExpr(p, bitsMulti)
            val sliceSize = mkBvSubExpr(eBit, sBit)
            val intersects = mkBvSignedGreaterExpr(sliceSize, mkBv(0, sliceSize.sort))
            val exprSizeBits = translatedSlice.expr.sort.sizeBits
            val exprSize : UExpr<UBvSort> = mkBv(exprSizeBits.toInt(), bv32Sort)
            val cutRight = mkBvSubExpr(exprSize, eBit)
            var res = mkBvShiftLeftExpr(translatedSlice.expr, extendOrExtract(cutRight, exprSizeBits))
            val cutLeft = extendOrExtract(mkBvAddExpr(cutRight, sBit), exprSizeBits)
            res = mkBvLogicalShiftRightExpr(res, cutLeft)
            res = extendOrExtract(res, sightTypeBitSize.toUInt())
            val shifted = mkBvShiftLeftExpr(res, extendOrExtract(pBit, sightTypeBitSize.toUInt()))
            mkIte(intersects, mkBvOrExpr(acc, shifted), acc)
        }
        return result as KExpr<Sort>
    }

    private fun extendOrExtract(expr: UExpr<UBvSort>, size: UInt): UExpr<UBvSort> = with(ctx) {
        val currentSize = expr.sort.sizeBits
        if (currentSize > size) {
            mkBvExtractExpr(high = (size - 1u).toInt(), low = 0, expr)
        } else {
            val diff = size - currentSize
            mkBvZeroExtensionExpr(diff.toInt(), expr)
        }
    }
}
