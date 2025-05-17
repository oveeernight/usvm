package org.usvm.machine.interpreter

import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.impl.*
import org.usvm.UCallStack
import org.usvm.machine.IlTypeSystem
import org.usvm.machine.state.IlState

fun IlStmt.enclosingFilter() : IlEhScope {
    val scopes = sortedEhcs()
    val index = location.index
    return scopes.first { it is IlFilterScope && it.fb.location.index <= index && index < it.hb.location.index }
}


fun IlStmt.isInFilter(): Boolean {
    val method = method
    val ehcs = method.scopes
    val index = location.index
    return ehcs.any { it is IlFilterScope && it.fb.location.index <= index && index < it.hb.location.index }
}

fun UCallStack<IlMethod, IlStmt>.findLastFilter(currStmt: IlStmt): Pair<IlEhScope, Int>? {
    val stacktrace = stackTrace(currStmt)
    val filterFrameIdx = stacktrace.indexOfLast { frame -> frame.instruction.isInFilter() }
    if (filterFrameIdx == -1) return null
    val frame = stacktrace[filterFrameIdx]
    return frame.instruction.enclosingFilter() to filterFrameIdx
}

fun IlMethodResult.Exception.findCatchOrFilter(
    callStack: UCallStack<IlMethod, IlStmt>,
    lastStmt: IlStmt,
    typeSystem: IlTypeSystem,
    previousCatchOrFilter: IlEhScope? = null
): Pair<IlEhScope, Int>? {
    var reachedPreviousHandler = previousCatchOrFilter == null
    val stacktrace = callStack.stackTrace(lastStmt)
    for (i in stacktrace.size - 1 downTo 0) {
        val stmt = stacktrace[i].instruction
        if (stmt.isInFilter()) {
            break
        }
        val ehcs = stmt.sortedEhcs()
        for (ehc in ehcs) {
            when {
                lastStmt.inHandlerBlock(ehc)-> { }
                ehc is IlCatchScope && ehc == previousCatchOrFilter -> {
                    reachedPreviousHandler = true
                }
                ehc is IlFilterScope && ehc == previousCatchOrFilter -> {
                    reachedPreviousHandler = true
                }
                ehc is IlCatchScope && reachedPreviousHandler -> {
                    if (typeSystem.isSupertype(supertype =  ehc.exceptionType, type)) return ehc to i
                }
                ehc is IlFilterScope && reachedPreviousHandler -> {
                    return ehc to i
                }
            }
        }
    }
    return null
}

fun IlStmt.findEnclosingFinallyOrFault(lastExecutedFinallyScope: IlEhScope? = null): IlEhScope? {
    val ehcs = if (lastExecutedFinallyScope == null)
        sortedEhcs()
    else sortedEhcs().dropWhile { it != lastExecutedFinallyScope }.drop(1)
    for (ehc in ehcs) {
        when (ehc) {
            is IlFinallyScope -> return ehc
            is IlFaultScope -> return ehc
        }
    }
    return null
}

private fun precedes(ehcs: List<IlEhScope>, ehc1: IlEhScope, ehc2: IlEhScope): Boolean {
    val idx1 = ehcs.indexOf(ehc1)
    val idx2 = ehcs.indexOf(ehc2)
    return idx1 < idx2
}

fun IlState.findNextFinallyOrFault(
    catchScope: IlEhScope,
    lastStmt: IlStmt,
    lastExecutedFinallyScope: IlEhScope? = null
): Pair<IlEhScope, Int>? {
    val stacktrace = callStack.stackTrace(lastStmt)
    for (i in stacktrace.size - 1 downTo 0) {
        val frame = stacktrace[i]
        val enclosingFinallyOrFault = frame.instruction.findEnclosingFinallyOrFault(lastExecutedFinallyScope)
        if (enclosingFinallyOrFault != null) {
            if (catchScope.he.method == enclosingFinallyOrFault.he.method) {
                // here we need to decide if the scope of finally is nested in try block of catch scope
                val ehcs = frame.instruction.sortedEhcs()
                if (precedes(ehcs, enclosingFinallyOrFault, catchScope)) {
                    return enclosingFinallyOrFault to i
                }
            } else {
                return enclosingFinallyOrFault to i
            }
        }

    }
    return null
}
