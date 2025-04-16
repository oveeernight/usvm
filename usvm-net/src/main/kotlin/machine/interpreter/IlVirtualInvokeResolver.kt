package org.usvm.machine.interpreter

import io.ksmt.utils.asExpr
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.panda.staticvm.ir.PandaRefTypeCheckInstIr
import org.usvm.UBoolExpr
import org.usvm.UConcreteHeapRef
import org.usvm.UHeapRef
import org.usvm.api.evalTypeEquals
import org.usvm.api.typeStreamOf
import org.usvm.machine.IlContext
import org.usvm.machine.state.IlState
import org.usvm.machine.state.newStmt
import org.usvm.memory.foldHeapRef
import org.usvm.model.UModelBase
import org.usvm.types.UTypeStream
import org.usvm.types.single

fun resolveVirtualInvoke(
    callStmt: IlVirtualCallStmt,
    ctx: IlContext,
    scope: IlStepScope,
    forkOnRemainingTypes: Boolean
) {
    val models = scope.calcOnState { models }
    if (models.isNotEmpty()) {
        resolveVirtualInvokeWithModel(callStmt, ctx, scope, models.first())
    }
    else {
        resolveVirtualInvokeWithoutModel(callStmt, ctx, scope, forkOnRemainingTypes)
    }
}

private fun resolveVirtualInvokeWithoutModel(
    callStmt: IlVirtualCallStmt,
    ctx: IlContext,
    scope: IlStepScope,
    forkOnRemainingTypes: Boolean
) {
    val instance = callStmt.args[0].asExpr(ctx.addressSort)

    val refsWithConditions : MutableList<Pair<UBoolExpr, UHeapRef>>  = mutableListOf()
    foldHeapRef(instance, Unit,
        initialGuard = ctx.trueExpr,
        ignoreNullRefs = true,
        collapseHeapRefs = false,
        staticIsConcrete = true,
        blockOnConcrete = { _, (ref, condition) ->
            refsWithConditions += condition to ref
        },
        blockOnSymbolic = { _, (ref, condition) ->
            refsWithConditions += condition to ref
        })

//    val conditionsWithBlock = refsWithConditions.flatMapTo(mutableListOf()) {
//        TODO()
//    }
    val typeStream = scope.calcOnState { memory.types.getTypeStream(instance) }
    TODO()
}

private fun IlVirtualCallStmt.makeConcreteCallsForPossibleTypes(
    scope: IlStepScope,
    ctx: IlContext,
    instance: UHeapRef,
    typeStream: UTypeStream<out IlType>,
    typeSelector: IlTypeSelector,
    forkOnRemainingTypes: Boolean
): List<Pair<UBoolExpr, (IlState) -> Unit>> {
    val state = scope.calcOnState { this }
    val concreteTypes = typeSelector.choose(typeStream)
    val typeConditions = concreteTypes.map { state.memory.types.evalTypeEquals(instance, it) }
    val conditionsWithBlocks = concreteTypes.mapIndexedTo(mutableListOf()) { i, t ->
        val condition = typeConditions[i]
        val block = { state: IlState ->
            val concreteMethod = t.findMethod(method)
            val concreteCall = toConcreteCallStmt(concreteMethod)
            state.newStmt(concreteCall)
        }
        condition to block
    }

    if (forkOnRemainingTypes) {
        val excludeForkedTypesCondition = ctx.mkAnd(typeConditions.map { ctx.mkNot(it) })
        conditionsWithBlocks += excludeForkedTypesCondition to { }
    }

    return conditionsWithBlocks
}

private fun resolveVirtualInvokeWithModel(
    callStmt: IlVirtualCallStmt,
    ctx: IlContext,
    scope: IlStepScope,
    model: UModelBase<IlType>
) {
    val instance = callStmt.args[0].asExpr(ctx.addressSort)
    val evaledInstance = model.eval(instance) as UConcreteHeapRef
    val concreteInvokes = callStmt.prepareInvokeOnConcreteRef(scope, evaledInstance, ctx.trueExpr)
    scope.forkMulti(concreteInvokes)
}

private fun IlVirtualCallStmt.prepareInvokeOnConcreteRef(
    scope: IlStepScope,
    concreteRef: UConcreteHeapRef,
    condition: UBoolExpr
) : List<Pair<UBoolExpr, (IlState) -> Unit>> {
    val type = scope.calcOnState { memory.typeStreamOf(concreteRef) }.single()
    val concreteMethod = type.findMethod(method)
    val state = { state: IlState ->
        val concreteCall = toConcreteCallStmt(concreteMethod)
        state.newStmt(concreteCall)
    }
    return listOf(condition to state)
}
