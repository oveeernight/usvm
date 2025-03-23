package org.usvm.machine.state

import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.PathNode
import org.usvm.UCallStack
import org.usvm.UState
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.constraints.UPathConstraints
import org.usvm.machine.IlContext
import org.usvm.machine.interpreter.IlMethodResult
import org.usvm.machine.IlTarget
import org.usvm.memory.UMemory
import org.usvm.memory.UnsafeMemory
import org.usvm.model.UModelBase
import org.usvm.targets.UTargetsSet

class IlState(
    ctx: IlContext,
    ownership: MutabilityOwnership,
    override val entrypoint: IlMethod,
    callStack: UCallStack<IlMethod, IlStmt> = UCallStack(),
    pathConstraints: UPathConstraints<IlType> = UPathConstraints(ctx, ownership),
    override val memory: IlMemory = IlMemory(ctx, ownership, pathConstraints.typeConstraints),
    models: List<UModelBase<IlType>> = listOf(),
    pathNode: PathNode<IlStmt> = PathNode.root(),
    forkPoints: PathNode<PathNode<IlStmt>> = PathNode.root(),
    var methodResult: IlMethodResult = IlMethodResult.BeforeCall,
    targets: UTargetsSet<IlTarget, IlStmt> = UTargetsSet.empty())
    : UState<IlType, IlMethod, IlStmt, IlContext, IlTarget, IlState>(
        ctx,
        ownership,
        callStack,
        pathConstraints,
        memory,
        models,
        pathNode,
        forkPoints,
        targets
    ) {

    override fun clone(newConstraints: UPathConstraints<IlType>?): IlState {
        val newThisOwnership = MutabilityOwnership()
        val cloneOwnership = MutabilityOwnership()
        val clonedConstraints = newConstraints?.also {
            this.pathConstraints.changeOwnership(newThisOwnership)
            it.changeOwnership(newThisOwnership)
        } ?: pathConstraints.clone(newThisOwnership, cloneOwnership)
        this.ownership = newThisOwnership
        return IlState(
            ctx,
            newThisOwnership,
            entrypoint,
            callStack.clone(),
            clonedConstraints,
            memory.clone(clonedConstraints.typeConstraints, newThisOwnership, cloneOwnership),
            models,
            pathNode,
            forkPoints,
            methodResult,
            targets.clone()
        )
    }

    override val isExceptional: Boolean
        get() = methodResult is IlMethodResult.Exception
}
