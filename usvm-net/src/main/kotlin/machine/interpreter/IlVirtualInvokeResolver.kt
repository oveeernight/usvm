package org.usvm.machine.interpreter

import io.ksmt.utils.asExpr
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.api.evalTypeEquals
import org.usvm.api.typeStreamOf
import org.usvm.machine.IlContext
import org.usvm.machine.IlManagedRef
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
    typeSelector: IlTypeSelector,
    forkOnRemainingTypes: Boolean
) {
    val models = scope.calcOnState { models }
    if (models.isNotEmpty()) {
        resolveVirtualInvokeWithModel(callStmt, ctx, scope, models.first(), typeSelector, forkOnRemainingTypes)
    }
    else {
        resolveVirtualInvokeWithoutModel(callStmt, ctx, scope, typeSelector, forkOnRemainingTypes)
    }
}

private fun resolveVirtualInvokeWithoutModel(
    callStmt: IlVirtualCallStmt,
    ctx: IlContext,
    scope: IlStepScope,
    typeSelector: IlTypeSelector,
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

    val conditionsWithBlock: List<Pair<UBoolExpr, (IlState) -> Unit>> =
        refsWithConditions.flatMapTo(mutableListOf()) { (c, ref) ->
        when {
            isAllocatedConcreteHeapRef(ref) -> callStmt.prepareInvokeOnConcreteRef(scope, ref, c)
            ref is USymbolicHeapRef -> {
                val typeStream = scope.calcOnState { memory.types.getTypeStream(instance) }
                callStmt.makeConcreteCallsForPossibleTypes(scope, ctx, ref, typeStream, typeSelector, forkOnRemainingTypes)

            }
            else -> error("resolveVirtualInvokeWithoutModel: unexpected ref $ref")
        }
    }
    scope.forkMulti(conditionsWithBlock)
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
    model: UModelBase<IlType>,
    typeSelector: IlTypeSelector,
    forkOnRemainingTypes: Boolean
) {
    val instance = callStmt.args[0].asExpr(ctx.addressSort).let {
        if (it is IlManagedRef<*>) {
            scope.calcOnState { memory.read(it.memoryKey) }
        } else it
    }
    val evaledInstance = model.eval(instance) as UConcreteHeapRef
    if (isAllocatedConcreteHeapRef(evaledInstance) || isStaticHeapRef(evaledInstance)) {
        val concreteInvoke = callStmt.prepareInvokeOnConcreteRef(scope, evaledInstance, ctx.trueExpr)
        scope.forkMulti(concreteInvoke)
        return
    }
    // ref is symbolic
    val typeStream = scope.calcOnState { model.typeStreamOf(evaledInstance) }
    val symbolicInvokes =
        callStmt.makeConcreteCallsForPossibleTypes(scope, ctx, instance.asExpr(ctx.addressSort), typeStream, typeSelector, forkOnRemainingTypes)
    scope.forkMulti(symbolicInvokes)
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
