package org.usvm.expressions

import io.ksmt.expr.KBitVec32Value
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import java.util.LinkedList
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

// TODO write custom linked list to avoid deep copy in addCut function
fun <T, R> Collection<T>.mapToLinkedList(transform: (T) -> R): LinkedList<R> {
    val list = LinkedList<R>()
    forEach { list.add(transform(it)) }
    return list
}

fun <Sort : USort> UContext<*>.mkSlice(expr: UExpr<Sort>, exprType: IlType, cuts: LinkedList<Cut>) =
        Slice(this, expr, exprType, cuts).simplify()

fun <Sort: USort> UContext<*>.addCut(slice: Slice<Sort>, cut: Cut) : Slice<Sort> {
    val list = slice.cuts.mapToLinkedList { it }
    list.addLast(cut)
    return Slice(this, slice.expr, slice.exprType, list).simplify()
}
fun <Sort: USort> UContext<*>.mkCombine(slices: List<Slice<out USort>>, sort: Sort, sightType: IlType) =
    Combine(this, slices.filter { it.cuts.size > 0 }, sort, sightType)


private fun <Sort: USort> Slice<Sort>.simplify() : Slice<Sort> {
    if (cuts.size == 0) return this
    var sliceIsValid = true
    val exprSize = exprType.size
    var start = 0
    var end = exprSize
    var pos = 0
    var posIsStable = false
    val symbolicCuts = LinkedList<Cut>()
    val ordered = cuts
    for (cut in ordered) {
        val concreteS = cut.start as? KBitVec32Value
        val concreteE = cut.end as? KBitVec32Value
        val concreteP = cut.pos as? KBitVec32Value
        if (!sliceIsValid || concreteS == null || concreteE == null || concreteP == null) {
            symbolicCuts.add(cut)
            continue
        }
        val cutLeft = max(concreteS.intValue - pos, 0)
        val cutRight = min(concreteE.intValue - pos, end)
        val cutSize = cutRight - cutLeft
        start += cutLeft
        end = min(start + cutSize, end)
        pos = if (cut.posIsStable) {
            max(pos, concreteP.intValue)
        } else {
            max(0, pos + concreteP.intValue)
        }
        posIsStable = cut.posIsStable

        if (end > start) {
            assert(start in 0..<exprSize)
            assert(end in 1..exprSize)
            assert(pos >= 0)
        } else {
            sliceIsValid = false
        }
    }

        return if (sliceIsValid) {
            val simplificationCut = with(ctx) {
                val s: UExpr<UBvSort> = mkBv(start, bv32Sort)
                val e: UExpr<UBvSort> = mkBv(end, bv32Sort)
                val p : UExpr<UBvSort> = mkBv(pos, bv32Sort)
                Cut(s, e, p, posIsStable)
            }
            val narrowed = start > 0 || pos != 0 || end < exprSize
            val shouldAddSimplifyCut = narrowed || symbolicCuts.size == 0 && cuts.size != 0
            if (narrowed || symbolicCuts.size == 0)
                symbolicCuts.addFirst(simplificationCut)
            Slice(ctx as UContext<*>, expr, exprType, symbolicCuts)
        } else {
            Slice(ctx as UContext<*>, expr, exprType, cuts = LinkedList())
        }
}
