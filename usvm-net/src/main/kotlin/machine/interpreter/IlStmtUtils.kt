package org.usvm.machine.interpreter

import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.impl.IlEhScope
import org.jacodb.api.net.ilinstances.impl.IlFilterScope


fun IlStmt.inTryBlock(eh: IlEhScope): Boolean {
    if (method != eh.he.method) return false
    val idx = location.index
    val tbIdx = eh.tb.location.index
    val teIdx = eh.te.location.index
    return idx in tbIdx..teIdx
}

fun IlStmt.inHandlerBlock(eh: IlEhScope): Boolean {
    if (method != eh.he.method) return false
    val idx = location.index
    val hbIdx = if (eh is IlFilterScope) eh.fb.location.index else eh.hb.location.index
    val heIdx = eh.he.location.index
    return idx in hbIdx..heIdx
}

fun IlStmt.inProtectedBlock(eh: IlEhScope): Boolean = inTryBlock(eh) || inHandlerBlock(eh)


fun IlStmt.sortedEhcs() : List<IlEhScope> {
    val method = method
    return method.scopes.filter { inProtectedBlock(it) }.sortedBy { it.he.location.index }
}
