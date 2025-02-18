package org.usvm.machine.interpreter

import io.ksmt.expr.KExpr
import io.ksmt.utils.asExpr
import org.example.ilinstances.IlMethod
import org.example.ilinstances.IlReferenceType
import org.example.ilinstances.IlType
import org.jacodb.api.net.ilinstances.*
import org.usvm.*
import org.usvm.api.allocateConcreteRef
import org.usvm.api.allocateStaticRef
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.forkblacklists.UForkBlackList
import org.usvm.machine.IlContext
import org.usvm.machine.IlMachineOptions
import org.usvm.machine.VoidValue
import org.usvm.machine.state.*
import org.usvm.machine.write
import org.usvm.memory.URegisterStackLValue
import org.usvm.solver.USatResult

typealias IlStepScope = StepScope<IlState, IlType, IlStmt, IlContext>


class IlInterpreter(
    private val ctx: IlContext,
    private val ilOptions: IlMachineOptions,
    val forkBlackList: UForkBlackList<IlState, IlStmt> = UForkBlackList.createDefault()
) : UInterpreter<IlState>() {

    private val strings = mutableMapOf<String, UConcreteHeapRef>()
    fun stringAllocator(string: String) = strings.getOrPut(string) {
        ctx.allocateConcreteRef()
    }

    private val typeInstances = mutableMapOf<IlType, UConcreteHeapRef>()
    private fun typesAlloactor(type: IlType): UConcreteHeapRef =
        typeInstances.getOrPut(type) { ctx.allocateStaticRef() }

    private val methodLocals = mutableMapOf<IlMethod, MutableMap<String, Int>>()
    private fun mapMethodLocals(method: IlMethod, local: IlLocal): Pair<Int, IlType> =
        when (local) {
            is IlArgument -> methodLocals.getOrPut(method) {
                mutableMapOf()
            }.getOrPut(local.name) { 0 } to local.paramType

            is IlLocalVar -> (method.paramsWithThisCount() + local.index) to local.type

            is IlErrVar -> (method.paramsWithThisCount() + method.locals.size + local.index) to local.type

            else -> error("mapMethodLocals: unexpected local $local")
        }


    fun getInitialState(method: IlMethod): IlState {
        // TODO refine when static, abstract modifiers will be ready
        val initOwnership = MutabilityOwnership()
        val state = IlState(ctx, initOwnership, method)

        with(ctx) {
            val thisLValue = URegisterStackLValue(addressSort, 0)
            val ref = state.memory.read(thisLValue).asExpr(addressSort)
            state.pathConstraints += ref neq nullRef
            // TODO type constraints on abstract


            val entrypointArgs = mutableListOf<Pair<IlType, UHeapRef>>()

            method.parametes.forEachIndexed { idx, param ->
                val type = param.paramType
                if (type is IlReferenceType) {
                    val paramLValue = URegisterStackLValue(typeToSort(type), idx + 1)
                    val paramRValue = state.memory.read(paramLValue).asExpr(addressSort)
                    val constr = ctx.mkIsSubtypeExpr(paramRValue, param.paramType)
                    state.pathConstraints += constr
                    entrypointArgs += type to paramRValue
                }
            }

            val solver = solver<IlType>()
            val model = (solver.check(state.pathConstraints) as USatResult).model
            state.models = listOf(model)

            state.callStack.push(method, returnSite = null)
            state.memory.stack.push(method.parametes.size, method.locals.size)

            TODO()
        }
    }

    override fun step(state: IlState): StepResult<IlState> {
        val stmt = state.lastStmt
        val scope = IlStepScope(state, forkBlackList)
        when (stmt) {
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
        
        return scope.stepResult()
    }

    private fun visitAssignStmt(scope: IlStepScope, stmt: IlAssignStmt) {
        val resolver = mkExprResolver(scope)
        val lvalue = resolver.resolveULValue(stmt.lhs)
        val rvalue = resolver.resolve(stmt.rhs)
        scope.doWithState {
            memory.write(lvalue, rvalue)
            newStmt(stmt.next(scope))
        }

        // TODO handle calls in rhs when cfg will be available
    }

    private fun visitGotoStmt(scope: IlStepScope, stmt: IlGotoStmt) {
        scope.doWithState {
            newStmt(stmt.target)
        }
    }

    private fun visitIfStmt(scope: IlStepScope, stmt: IlIfStmt) {
        val resolver = mkExprResolver(scope)
        val condition = resolver.resolve(stmt.condition).asExpr(ctx.boolSort)
        val (posStmt, negStmt) = stmt.target to stmt.next(scope)
        scope.forkWithBlackList(condition, posStmt, negStmt,
            blockOnTrueState = { newStmt(posStmt) },
            blockOnFalseState = { newStmt(negStmt) }
        )
    }

    private fun visitCallStmt(scope: IlStepScope, stmt: IlCallStmt) {
        TODO()
    }

    private fun visitCalliStmt(scope: IlStepScope, stmt: IlCalliStmt) {
        TODO()
    }

    private fun visitReturnStmt(scope: IlStepScope, stmt: IlReturnStmt) {
        val resolver = mkExprResolver(scope)
        val value = stmt.value?.let { resolver.resolve(it) }
            ?: ctx.void
        scope.doWithState {
            returnValue(value)
        }
    }

    private fun visitThrowStmt(scope: IlStepScope, stmt: IlThrowStmt) {
        TODO()
    }

    private fun visitRethrowStmt(scope: IlStepScope, stmt: IlRethrowStmt) {
        TODO()
    }

    private fun visitEndFaultStmt(scope: IlStepScope, stmt: IlEndFaultStmt) {
        TODO()
    }

    private fun visitEndFilterStmt(scope: IlStepScope, stmt: IlEndFilterStmt) {
        TODO()
    }

    private fun visitEndFinallyStmt(scope: IlStepScope, stmt: IlEndFinallyStmt) {
        TODO()
    }

    //TODO inefficient
    private fun IlStmt.next(stepScope: IlStepScope) = stepScope.calcOnState {
        val method = callStack.lastMethod()
        val body = method.body
        val currIndex = body.indexOf(this@next)
        body[currIndex + 1]
    }

    private fun mkExprResolver(scope: IlStepScope) =
        IlExprResolver(ctx, scope, ilOptions, ::stringAllocator, ::typesAlloactor, ::mapMethodLocals)
}
