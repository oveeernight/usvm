package org.usvm.machine.state

import org.example.ilinstances.IlMethod
import org.example.ilinstances.IlType
import org.jacodb.api.net.ilinstances.IlExpr
import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.*
import org.usvm.api.allocateConcreteRef
import org.usvm.machine.IlMethodResult

val IlState.lastStmt get() = pathNode.statement
fun IlState.newStmt(stmt: IlStmt) {
    pathNode += stmt
}

fun IlState.returnValue(value: UExpr<out USort>) {
    val method = callStack.lastMethod()
    val res = IlMethodResult.Success(value, method)
    methodResult = res
    val returnSite = callStack.pop()
    returnSite?.let { memory.stack.pop(); newStmt(it) }
}

fun IlMethod.toLocalIdx(idx: Int): Int = if (this.resolved) idx else idx + 1
fun IlMethod.paramsWithThisCount() : Int = toLocalIdx(parametes.size)

fun IlState.throwException(type: IlType, frame: UStackTraceFrame<IlMethod, IlStmt>) {
    val ref = ctx.allocateConcreteRef()
    memory.types.allocate(ref.address, type)
    methodResult = IlMethodResult.Exception(ref, type, frame.method, frame.instruction)
}
