package org.usvm.machine.interpreter

import org.jacodb.api.net.ilinstances.IlEhStmt
import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.impl.IlEhScope


fun IlStmt.inEhBlock(eh: IlEhScope): Boolean {
    val idx = location.index
    val tbIdx = eh.tb.location.index
    val teIdx = eh.tb.location.index
    val hbIdx = eh.hb.location.index
    val heIdx = eh.he.location.index
    return idx in tbIdx..teIdx || idx in hbIdx..heIdx
}


fun IlStmt.exceptionHandlers() : List<IlEhScope> {
    val method = method
    return method.scopes.filter { this.inEhBlock(it) }.sortedBy { it.he.location.index }
}
