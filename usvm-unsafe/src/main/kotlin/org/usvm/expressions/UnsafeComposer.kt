package org.usvm.org.usvm.expressions

import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UBvSort
import org.usvm.UComposer
import org.usvm.UContext
import org.usvm.USort
import org.usvm.UExpr
import org.usvm.isTrue
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.expressions.Combine
import org.usvm.expressions.Cut
import org.usvm.expressions.Slice
import org.usvm.expressions.mapToLinkedList
import org.usvm.memory.UReadOnlyMemory

open class UnsafeComposer<Type, USizeSort : USort>(
    override val ctx: UContext<USizeSort>,
    memory: UReadOnlyMemory<Type>,
    ownership: MutabilityOwnership
) : UComposer<Type, USizeSort>(ctx, memory, ownership), UnsafeTransformer<Type, USizeSort> {
    override fun <Sort : USort> transform(slice: Slice<Sort>): Slice<Sort> {
        val expr = slice.expr.accept(this)
        val cuts = slice.cuts.mapNotNull { cut ->
            val s = compose(cut.start)
            val e = compose(cut.end)
            val p = compose(cut.pos)
            if (cutIsValid(s, e)) {
                Cut(s, e, p, cut.posIsStable)
            } else null
        }.mapToLinkedList { it }

        return Slice(ctx, expr, slice.exprType, cuts)
    }

    override fun <Sort : USort> transform(combine: Combine<Sort>): UExpr<Sort> {
        val cuts = combine.slices.map { slice -> transform(slice) }
        val sort = combine.sort
        require(combine.sort is UBvSort)
        val composedCombine = Combine(ctx, cuts, combine.sort, combine.sightType)
        val translator = UnsafeTranslator<IlType, USizeSort>(ctx)
        val combineAsBv = translator.transform(composedCombine)
        return combineAsBv.cast()
    }

    private fun cutIsValid(s: UExpr<UBvSort>, e: UExpr<UBvSort>) = with(s.ctx) {
        val lengthIsZero = mkBvSignedGreaterOrEqualExpr(s, e).isTrue
        val endIsNegative = mkBvUnsignedLessOrEqualExpr(e, mkBv(0, s.sort)).isTrue
        !lengthIsZero && !endIsNegative
    }

}
