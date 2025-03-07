package org.usvm.org.usvm.expressions

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
import org.usvm.memory.UReadOnlyMemory

open class UnsafeComposer<Type, USizeSort : USort>(
    override val ctx: UContext<USizeSort>,
    memory: UReadOnlyMemory<Type>,
    ownership: MutabilityOwnership
) : UComposer<Type, USizeSort>(ctx, memory, ownership), UnsafeTransformer<Type, USizeSort> {
    override fun <Sort : USort> transform(slice: Slice<Sort>): Slice<Sort> {
        val expr = slice.expr.accept(this)
        val cuts = slice.cuts.mapNotNull { cut ->
            val s = cut.start.accept(this)
            val e = cut.end.accept(this)
            val p = cut.pos.accept(this)
            if (cutIsValid(s, e, p)) {
                Cut(s, e, p, cut.posIsStable)
            } else null
        }.mapToLinkedList { it }

        return Slice(ctx, expr, cuts)
    }

    override fun <Sort : USort> transform(combine: Combine<Sort>): UExpr<Sort> {
        val cuts = combine.slices.map { slice -> transform(slice) }
        return Combine(ctx, cuts, combine.sightType)
    }

    private fun cutIsValid(s: UExpr<UBvSort>, e: UExpr<UBvSort>, p: UExpr<UBvSort>) = with(s.ctx) {
        val lengthIsZero = mkBvUnsignedGreaterOrEqualExpr(s, e).isTrue
        val endIsNegative = mkBvUnsignedLessOrEqualExpr(s, mkBv(0, s.sort)).isTrue
        !lengthIsZero && !endIsNegative
    }

}
