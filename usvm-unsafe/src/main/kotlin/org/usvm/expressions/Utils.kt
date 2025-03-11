package org.usvm.org.usvm.expressions

import org.jacodb.api.common.CommonType
import org.usvm.*
import org.usvm.expressions.Combine
import org.usvm.expressions.Cut
import org.usvm.expressions.Slice
import java.util.LinkedList

fun <T, R> Collection<T>.mapToLinkedList(transform: (T) -> R): LinkedList<R> {
    val list = LinkedList<R>()
    forEach { list.add(transform(it)) }
    return list
}

fun <Sort: USort> UContext<*>.mkSlice(expr: UExpr<Sort>, cuts: LinkedList<Cut>) = Slice(this, expr, cuts)
fun <Sort: USort> UContext<*>.addCut(slice: Slice<Sort>, cut: Cut) : Slice<Sort> {
    val list = slice.cuts.mapToLinkedList { it }
    list.add(cut)
    return Slice(this, slice.expr, list)
}
fun <Sort : USort> UContext<*>.mkCombine(slices: List<Slice<Sort>>, sightType: CommonType) =
    Combine(this, slices, sightType)
