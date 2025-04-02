package org.usvm.machine.state

import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlMethodImpl
import org.usvm.UCallStack
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.UStackTraceFrame
import org.usvm.api.allocateConcreteRef
import org.usvm.machine.interpreter.IlConcreteCallStmt
import org.usvm.machine.interpreter.IlMethodResult

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

//fun IlMethod.toLocalIdx(idx: Int): Int = if (this.isStatic) idx else idx + 1
//fun IlMethod.paramsWithThisCount() : Int = toLocalIdx(parameters.size)
fun IlMethod.localsCount() : Int {
    this as IlMethodImpl
    return locals.size + temps.size + errs.size
}

fun IlMethod.typeOfRegister(reg: Int) : IlType {
    this as IlMethodImpl
    val paramsCount = parameters.size
    val localsCount = locals.size
    val tempsCount = temps.size
    val errsCount = errs.size
    return when {
        reg < paramsCount -> parameters[reg].type
        reg < paramsCount + localsCount -> locals[reg - paramsCount].type
        reg < paramsCount + localsCount + tempsCount -> temps[reg - paramsCount - localsCount].type
        reg < paramsCount + localsCount + tempsCount + errsCount -> errs[reg - paramsCount - localsCount - tempsCount].type
        else -> error("Unexpected reg $reg")
    }

}

fun IlState.throwException(type: IlType, frame: UStackTraceFrame<IlMethod, IlStmt>) {
    val ref = ctx.allocateConcreteRef()
    memory.types.allocate(ref.address, type)
    methodResult = IlMethodResult.Exception(ref, type, frame.method, frame.instruction)
}

fun IlState.insertConcreteCallStmt(method: IlMethod, args: List<UExpr<out USort>>) =
    newStmt(IlConcreteCallStmt(method, args, currentStatement))

fun IlState.callMethod(method: IlMethod, args: List<UExpr<out USort>>, returnSite: IlStmt) {
    callStack.push(method, returnSite)
    memory.stack.push(args.toTypedArray(), method.localsCount())
    newStmt(method.instList.first())
}
