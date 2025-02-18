package org.usvm.state

import org.example.ilinstances.IlMethod
import org.example.ilinstances.IlType
import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.PathNode
import org.usvm.UCallStack
import org.usvm.UState
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.constraints.UPathConstraints
import org.usvm.machine.IlContext
import org.usvm.machine.IlMethodResult
import org.usvm.machine.IlTarget
import org.usvm.memory.UMemory
import org.usvm.model.UModelBase
import org.usvm.targets.UTargetsSet

class IlState(
    ctx: IlContext,
    ownership: MutabilityOwnership,
    override val entrypoint: IlMethod,
    callStack: UCallStack<IlMethod, IlStmt> = UCallStack(),
    pathConstraints: UPathConstraints<IlType>,
    memory: UMemory<IlType, IlMethod> = UMemory(ctx, ownership, pathConstraints.typeConstraints),
    models: List<UModelBase<IlType>> = listOf(),
    pathNode: PathNode<IlStmt> = PathNode.root(),
    forkPoints: PathNode<PathNode<IlStmt>> = PathNode.root(),
    var methodResult: IlMethodResult = IlMethodResult.NoCall,
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
        TODO("Not yet implemented")
    }

    override val isExceptional: Boolean
        get() = TODO("Not yet implemented")
}
