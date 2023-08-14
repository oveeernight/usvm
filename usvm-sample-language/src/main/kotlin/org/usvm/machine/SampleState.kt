package org.usvm.machine

import org.usvm.UCallStack
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.UState
import org.usvm.constraints.UPathConstraints
import org.usvm.language.Field
import org.usvm.language.Method
import org.usvm.language.ProgramException
import org.usvm.language.SampleType
import org.usvm.language.Stmt
import org.usvm.language.argumentCount
import org.usvm.language.localsCount
import org.usvm.memory.UMemoryBase
import org.usvm.model.UModelBase
import org.usvm.PathsTrieNode

class SampleState(
    ctx: UContext,
    callStack: UCallStack<Method<*>, Stmt> = UCallStack(),
    pathConstraints: UPathConstraints<SampleType, UContext> = UPathConstraints(ctx),
    memory: UMemoryBase<Field<*>, SampleType, Method<*>> = UMemoryBase(ctx, pathConstraints.typeConstraints),
    models: List<UModelBase<Field<*>, SampleType>> = listOf(),
    pathLocation: PathsTrieNode<SampleState, Stmt> = ctx.mkInitialLocation(),
    var returnRegister: UExpr<out USort>? = null,
    var exceptionRegister: ProgramException? = null,
) : UState<SampleType, Field<*>, Method<*>, Stmt, UContext, SampleState>(
    ctx,
    callStack,
    pathConstraints,
    memory,
    models,
    pathLocation
) {
    override fun clone(newConstraints: UPathConstraints<SampleType, UContext>?): SampleState {
        val clonedConstraints = newConstraints ?: pathConstraints.clone()
        return SampleState(
            pathConstraints.ctx,
            callStack.clone(),
            clonedConstraints,
            memory.clone(clonedConstraints.typeConstraints),
            models,
            pathLocation,
            returnRegister,
            exceptionRegister
        )
    }

    override val isExceptional: Boolean
        get() = exceptionRegister != null
}

val SampleState.lastStmt: Stmt get() = pathLocation.statement
fun SampleState.newStmt(stmt: Stmt) {
    pathLocation = pathLocation.pathLocationFor(stmt, this)
}

fun SampleState.popMethodCall(valueToReturn: UExpr<out USort>?) {
    val returnSite = callStack.pop()
    if (callStack.isNotEmpty()) {
        memory.stack.pop()
    }

    returnRegister = valueToReturn

    if (returnSite != null) {
        newStmt(returnSite)
    }
}

fun SampleState.addEntryMethodCall(
    applicationGraph: SampleApplicationGraph,
    method: Method<*>,
) {
    val entryPoint = applicationGraph.entryPoints(method).single()
    callStack.push(method, returnSite = null)
    memory.stack.push(method.argumentCount, method.localsCount)
    newStmt(entryPoint)
}

fun SampleState.addNewMethodCall(
    applicationGraph: SampleApplicationGraph,
    method: Method<*>,
    arguments: List<UExpr<out USort>>,
) {
    val entryPoint = applicationGraph.entryPoints(method).single()
    val returnSite = lastStmt
    callStack.push(method, returnSite)
    memory.stack.push(arguments.toTypedArray(), method.localsCount)
    newStmt(entryPoint)
}
