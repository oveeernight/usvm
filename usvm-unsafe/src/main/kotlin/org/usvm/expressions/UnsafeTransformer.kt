package org.usvm.org.usvm.expressions

import org.usvm.UExpr
import org.usvm.USort
import org.usvm.UTransformer
import org.usvm.expressions.Combine
import org.usvm.expressions.Slice

interface UnsafeTransformer<Type, USizeSort : USort> : UTransformer<Type, USizeSort> {
    fun <Sort: USort> transform(slice: Slice<Sort>): UExpr<Sort>
    fun <Sort: USort> transform(combine: Combine<Sort>): UExpr<Sort>
}
