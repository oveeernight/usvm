package org.usvm.machine

import org.example.ilinstances.IlType
import org.jacodb.api.net.ilinstances.*
import org.usvm.StepResult
import org.usvm.StepScope
import org.usvm.UInterpreter
import org.usvm.forkblacklists.UForkBlackList
import org.usvm.state.IlState
import org.usvm.state.lastStmt

typealias IlStepScope = StepScope<IlState, IlType, IlStmt, IlContext>

class IlInterpreter(
    private val ctx: IlContext,
    val forkBlackList: UForkBlackList<IlState, IlStmt> = UForkBlackList.createDefault()
) : UInterpreter<IlState>() {
    override fun step(state: IlState): StepResult<IlState> {
        val stmt = state.lastStmt
        val scope = IlStepScope(state, forkBlackList)
        return when (stmt) {
            is IlAssignStmt -> visitAssignStmt(scope, stmt)
            is IlGotoStmt -> visitGotoStmt(scope, stmt)
            is IlIfStmt -> visitIfStmt(scope, stmt)
            is IlCallStmt -> visitCallStmt(scope, stmt)
            is IlCalliStmt -> visitCalliStmt(scope, stmt)
            is IlReturnStmt -> visitReturnStmt(scope, stmt)
            is IlThrowStmt -> visitThrowStmt(scope, stmt)
            is IlRethrowStmt -> visitRethrowStmt(scope, stmt)
            is IlEndFaultStmt -> visitEndFaultStmt(scope, stmt)
            is IlEndFilterStmt -> visitEndFilterStmt(scope, stmt)
            is IlEndFinallyStmt -> visitEndFinallyStmt(scope, stmt)
            else -> error("Unknown statement: $stmt")
        }
    }

    private fun visitAssignStmt(scope: IlStepScope, stmt: IlAssignStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitGotoStmt(scope: IlStepScope, stmt: IlGotoStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitIfStmt(scope: IlStepScope, stmt: IlIfStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitCallStmt(scope: IlStepScope, stmt: IlCallStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitCalliStmt(scope: IlStepScope, stmt: IlCalliStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitReturnStmt(scope: IlStepScope, stmt: IlReturnStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitThrowStmt(scope: IlStepScope, stmt: IlThrowStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitRethrowStmt(scope: IlStepScope, stmt: IlRethrowStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitEndFaultStmt(scope: IlStepScope, stmt: IlEndFaultStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitEndFilterStmt(scope: IlStepScope, stmt: IlEndFilterStmt): StepResult<IlState> {
        TODO()
    }

    private fun visitEndFinallyStmt(scope: IlStepScope, stmt: IlEndFinallyStmt): StepResult<IlState> {
        TODO()
    }

}
