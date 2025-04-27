package org.usvm.machine.interpreter

import io.ksmt.utils.asExpr
import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.*
import org.jacodb.api.net.ilinstances.impl.IlArrayType
import org.jacodb.api.net.ilinstances.impl.IlMethodImpl
import org.jacodb.api.net.ilinstances.impl.IlStructType
import org.usvm.*
import org.usvm.api.allocateStaticRef
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.array.length.UArrayLengthLValue
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.forkblacklists.UForkBlackList
import org.usvm.machine.*
import org.usvm.machine.state.*
import org.usvm.solver.USatResult

typealias IlStepScope = StepScope<IlState, IlType, IlStmt, IlContext>

@Suppress("UNUSED_PARAMETER")
class IlInterpreter(
    private val ctx: IlContext,
    private val appGraph: IlApplicationGraph,
    private val ilOptions: IlMachineOptions,
    val forkBlackList: UForkBlackList<IlState, IlStmt> = UForkBlackList.createDefault()
) : UInterpreter<IlState>() {

    private val strings = mutableMapOf<String, UConcreteHeapRef>()

    private val typeInstances = mutableMapOf<IlType, UConcreteHeapRef>()
    private fun typesAllocator(type: IlType): UConcreteHeapRef =
        typeInstances.getOrPut(type) { ctx.allocateStaticRef() }

    private val methodLocals = mutableMapOf<IlMethod, MutableMap<String, Int>>()
    private fun mapMethodLocals(method: IlMethod, local: IlLocal): Pair<Int, IlType> =
        when (local) {
            is IlArgument -> methodLocals.getOrPut(method) {
                mutableMapOf()
            }.getOrPut(local.name) { local.index } to local.type

            is IlLocalVar -> (method.parameters.size + local.index) to local.type

            is IlTempVar -> (method.parameters.size + (method as IlMethodImpl).locals.size + local.index) to local.type

//            is IlErrVar -> (method.paramsWithThisCount() + method .size + local.index) to local.type

            else -> error("mapMethodLocals: unexpected local ${local.type}")
        }


    fun getInitialState(method: IlMethod): IlState {
        // TODO refine when static, abstract modifiers will be ready
        val initOwnership = MutabilityOwnership()
        val state = IlState(ctx, initOwnership, method)

        with(ctx) {
            // TODO type constraints on abstract
            if (!method.isStatic) {
                val thisLValue = IlRegisterStackLValue(addressSort, 0, 0)
                val ref = state.memory.read(thisLValue).asExpr(addressSort)
                state.pathConstraints += !mkHeapRefEq(ref, nullRef)
            }
            val entrypointArgs = mutableListOf<Pair<IlType, UExpr<out USort>>>()

            method.parameters.forEachIndexed { idx, param ->
                val type = param.type
                val paramLValue = IlRegisterStackLValue(typeToSort(type), 0, idx)
                val paramRValue = state.memory.read(paramLValue)
                if (!isPrimitiveType(type)) {
                    val refinedRValue = if (type is IlStructType) {
                        state.copyStruct(paramRValue.asExpr(ctx.addressSort), type)
                    } else paramRValue.asExpr(ctx.addressSort)
                    state.pathConstraints += mkIsSubtypeExpr(refinedRValue, type)
                    entrypointArgs += type to refinedRValue
                } else entrypointArgs += type to paramRValue
            }

            val solver = solver<IlType>()
            val model = (solver.check(state.pathConstraints) as USatResult).model
            state.models = listOf(model)

            state.callStack.push(method, returnSite = null)
            method as? IlMethodImpl ?: error("Unexpected method type for now")
            val localsSize = method.locals.size + method.temps.size + method.errs.size
            val params = entrypointArgs.map { (_, a) -> a}.toTypedArray()
            state.memory.stack.push(params, localsSize)
            state.newStmt(IlMethodEntryPointStmt(method, entrypointArgs))
        }

        return state
    }

    override fun step(state: IlState): StepResult<IlState> {
        val stmt = state.currentStatement
        val scope = IlStepScope(state, forkBlackList)
        //TODO  handle exceptions
//        if (state.methodResult is IlMethodResult.Exception) return scope.stepResult()

        when (stmt) {
            is TransparentMethodCallBaseStmt -> visitTransparentCall(scope, stmt)
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

    private fun visitTransparentCall(scope: IlStepScope, stmt: TransparentMethodCallBaseStmt) {
//        val resolver = mkExprResolver(scope)
        when (stmt) {
            is IlMethodEntryPointStmt -> {
                // TODO init statics someday
                scope.doWithState { newStmt(appGraph.entryPoints(stmt.method).first()) }
            }

            is IlConcreteCallStmt -> {
                scope.doWithState { callMethod(stmt.method, stmt.args, stmt.returnSite) }
            }

            is IlVirtualCallStmt -> resolveVirtualCall(stmt, scope)
            else -> error("visitTransparentCall: unexpected call ${stmt.method}")
        }
    }

    // TODO handle calls in rhs when cfg will be available
    private fun visitAssignStmt(scope: IlStepScope, stmt: IlAssignStmt) {
        val resolver = mkExprResolver(scope)
        val lhv = stmt.lhv
        scope.doWithState {
            val rhvType = stmt.rhv.type
            val rvalue = resolver.resolve(stmt.rhv)?.let { if (stmt.rhv !is IlNewExpr && rhvType is IlStructType) {
                copyStruct(it.asExpr(ctx.addressSort), rhvType)
            }  else it } ?: return@doWithState
            if (lhv is IlUnmanagedDerefExpr) {
                val ptr = resolver.resolve(lhv.value)
                require(ptr is IlPtr<*>)
                checkAccessViolation(scope, ptr)
                memory.writeUnsafe(ptr, rvalue, rhvType)
            } else {
                val lvalue = resolver.resolveLValue(stmt.lhv) ?: return@doWithState
//              val rvalue = if (stmt.lhv.type != stmt.rhv.type) {
//                  val convCast = IlConvCastExpr(stmt.lhv.type, stmt.rhv)
//                  resolver.resolve(convCast) ?: return
//              } else {
//                  resolver.resolve(stmt.rhv) ?: return
//              }
                memory.write(lvalue, rvalue)
            }
            newStmt(stmt.next())
        }
    }

    private fun checkAccessViolation(scope: IlStepScope, ptr: IlPtr<*>) = with(ctx) {
        val locationSize: UExpr<UBvSort> = when (val locType = ptr.locationType) {
            is IlArrayType -> {
                val key = ptr.base
                require(key is UArrayIndexLValue<*, *, *>)
                val arrayRef = key.ref
                val desc = arrayDescriptorOf(locType)
                val length = scope.calcOnState { memory.read(UArrayLengthLValue(arrayRef, desc, sizeSort)) }
                val elemSize = mkBv(locType.elementType.size, sizeSort)
                mkBvMulExpr(length, elemSize).cast()
            }
            else -> mkBv(ptr.locationType.size, bv32Sort)
        }
        val viewTypeSize : UExpr<UBvSort> = mkBv(ptr.sightType.size, sizeSort)
        val zero : UExpr<UBvSort> = mkBv(0, sizeSort)
        val startByte = ptr.offset
        val endByte = mkBvAddExpr(startByte, viewTypeSize)
        val accessViolationCondition =
            mkOr(
                mkBvSignedLessExpr(startByte, zero),
                mkBvSignedLessOrEqualExpr(endByte, zero),
                mkBvSignedGreaterOrEqualExpr(startByte, locationSize),
                mkBvSignedGreaterExpr(endByte, locationSize),
            )
        scope.fork(accessViolationCondition,
            blockOnTrueState = { criticalErrorOccurred = true },
            blockOnFalseState = { }
        )
    }

    private fun visitGotoStmt(scope: IlStepScope, stmt: IlGotoStmt) {
        scope.doWithState {
            val target = stmt.method.instList[stmt.target]
            newStmt(target)
        }
    }

    private fun visitIfStmt(scope: IlStepScope, stmt: IlIfStmt) {
        val resolver = mkExprResolver(scope)
        val condition = resolver.resolve(stmt.condition)?.asExpr(ctx.boolSort) ?: return
        val (posStmt, negStmt) = stmt.method.instList[stmt.target] to stmt.next()
        scope.forkWithBlackList(condition, posStmt, negStmt,
            blockOnTrueState = { newStmt(posStmt) },
            blockOnFalseState = { newStmt(negStmt) }
        )
    }

    private fun visitCallStmt(scope: IlStepScope, stmt: IlCallStmt) {
        val resolver = mkExprResolver(scope)
        resolver.resolve(stmt.call) ?: return
        scope.doWithState {
            newStmt(stmt.next())
        }
    }

    private fun visitCalliStmt(scope: IlStepScope, stmt: IlCalliStmt) {
        TODO()
    }

    private fun visitReturnStmt(scope: IlStepScope, stmt: IlReturnStmt) {
        val resolver = mkExprResolver(scope)
        val value = stmt.returnValue?.let { resolver.resolve(it) }
            ?: ctx.void
        scope.doWithState {
            returnValue(value)
        }
    }

    private fun visitThrowStmt(scope: IlStepScope, stmt: IlThrowStmt) {
//        val resolver = mkExprResolver(scope)
//        val exception = resolver.resolve(stmt.value)?.asExpr(ctx.addressSort) ?: return
        scope.doWithState {
            throwException(stmt.value.type, callStack.stackTrace(currentStatement).last())
        }

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

    private fun resolveVirtualCall(callStmt: IlVirtualCallStmt, scope: IlStepScope) {
        val typeSelector = IlFixedInheritorsNumberTypeSelector()
        resolveVirtualInvoke(callStmt, ctx, scope, typeSelector, ilOptions.forkOnRemainingTypes)
    }

    private fun IlStmt.next() : IlStmt = location.method.instList[location.index + 1]

    private fun mkExprResolver(scope: IlStepScope) =
        IlExprResolver(ctx, scope, ilOptions, strings, ::typesAllocator, ::mapMethodLocals)
}
