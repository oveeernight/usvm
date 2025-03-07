package org.usvm.org.usvm.expressions

import org.usvm.UBvSort
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.expressions.Cut
import org.usvm.expressions.Slice
import java.util.LinkedList

fun <T, R> Collection<T>.mapToLinkedList(transform: (T) -> R): LinkedList<R> {
    val list = LinkedList<R>()
    forEach { list.add(transform(it)) }
    return list
}

fun <Sort: USort> UContext<*>.mkSlice(expr: UExpr<Sort>, cuts: LinkedList<Cut>) = Slice<Sort>(this, expr, cuts)
