package org.usvm.machine.interpreter

import io.ksmt.utils.asExpr
import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.*
import org.jacodb.api.net.ilinstances.impl.*
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
    private val forkBlackList: UForkBlackList<IlState, IlStmt> = UForkBlackList.createDefault()
) : UInterpreter<IlState>() {

    private val strings = mutableMapOf<String, UConcreteHeapRef>()

    private val typeInstances = mutableMapOf<IlType, UConcreteHeapRef>()
    private fun typesAllocator(type: IlType): UConcreteHeapRef =
        typeInstances.getOrPut(type) { ctx.allocateStaticRef() }

    private val methodLocals = mutableMapOf<IlMethod, MutableMap<IlLocal, Int>>()
    private fun mapMethodLocals(method: IlMethod, local: IlLocal): Int =
      methodLocals.getOrPut(method) { mutableMapOf() }.let { varsMapping ->
          varsMapping.getOrPut(local) { method.mapLocalToRegisterStackIdx(local) }
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

            state.callStack.push(method, returnSite = null, retSiteFrameIndex = -1)
            method as? IlMethodImpl ?: error("Unexpected method type for now")
            val localsSize = method.locals.size + method.temps.size + method.errs.size
            val params = entrypointArgs.map { (_, a) -> a}.toTypedArray()
            state.memory.stack.push(params, localsSize)
//            state.callStack.push(method, returnSite = null, retSiteFrameIndex = -1)
            state.initializeStructLocals(method, ::mapMethodLocals)
            state.newStmt(IlMethodEntryPointStmt(method, entrypointArgs))
        }

        return state
    }

    override fun step(state: IlState): StepResult<IlState> {
        val methodResult = state.methodResult
        val stmt = state.currentStatement
        org.usvm.logger.error { stmt }
        val scope = IlStepScope(state, forkBlackList)
        if (methodResult is IlMethodResult.Exception) {
            handleException(methodResult, scope)
            return scope.stepResult()
        }

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
            is IlEndFaultStmt -> visitEndFinallyOrFaultStmt(scope, stmt)
            is IlEndFinallyStmt -> visitEndFinallyOrFaultStmt(scope, stmt)
            is IlEndFilterStmt -> visitEndFilterStmt(scope, stmt)
            else -> error("Unknown statement: $stmt")
        }
        
        return scope.stepResult()
    }


    private fun IlState.handleException(exception: IlMethodResult.Exception, previousCheckedScope: IlEhScope? = null) {
        val ilTypeSystem = ctx.typeSystem<IlType>() as IlTypeSystem
        val throwStmt = exception.stmt
        val catchAndFrameIndex = exception.findCatchOrFilter(callStack, throwStmt, ilTypeSystem, previousCheckedScope)
        when  {
            catchAndFrameIndex == null -> {
                // if an exception occurred in a filter scope and no handler was found, we drop
                // current exception and continue search for handler of previous exception
                // otherwise we terminate state
                val lastFilter = callStack.findLastFilter(exception.stmt)
                if (lastFilter != null) {
                    val (filter, filterFrameIdx) = lastFilter
                    exceptionsStack.removeLast()
                    dropFramesAfterIndex(filterFrameIdx)
                    val previousException = exceptionsStack.last().exception
                    methodResult = previousException
                    handleException(previousException, filter)
                } else {
                    terminate()
                }
            }

            catchAndFrameIndex.first is IlCatchScope -> {
                val (catch, catchFrameIdx) = catchAndFrameIndex
                val nextFinally = findNextFinallyOrFault(catch, exception.stmt, previousCheckedScope)

                exceptionsStack.removeLast()
                exceptionsStack.add(CaughtExceptionEntry(exception, catch, catchFrameIdx))
                methodResult = IlMethodResult.BeforeCall

                if (nextFinally == null) {
                    dropFramesAfterIndex(catchFrameIdx)
                    newStmt(catch.hb)
                } else {
                    val (finally, finallyFrameIdx) = nextFinally
                    // both statements are necessary. even if we have no frames to drop, we may
                    // observe filter on another frame
                    dropFramesAfterIndex(finallyFrameIdx)
                    memory.stack.observingFrame = finallyFrameIdx
                    newStmt(finally.hb)
                }
            }

            catchAndFrameIndex.first is IlFilterScope -> {
                val (filter, frameIdx) = catchAndFrameIndex
                filter as IlFilterScope
                methodResult = IlMethodResult.BeforeCall
                memory.stack.observingFrame = frameIdx
                newStmt(filter.fb)
            }
        }
    }

    private fun handleException(
        exception: IlMethodResult.Exception, stepScope: IlStepScope
    ) {
        val state = stepScope.calcOnState { this }
        state.handleException(exception)
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
            var rvalue =
                if (rhvType != lhv.type) {
                    val convCast = IlConvCastExpr(stmt.lhv.type, stmt.rhv)
                  resolver.resolve(convCast) ?: return@doWithState
                } else {
                  resolver.resolve(stmt.rhv) ?: return@doWithState
                }
            if (stmt.rhv !is IlNewExpr && rhvType is IlStructType) {
                rvalue = copyStruct(rvalue.asExpr(ctx.addressSort), rhvType)
            }
            if (lhv is IlUnmanagedDerefExpr) {
                val ptr = resolver.resolve(lhv.value)
                require(ptr is IlPtr<*>)
                checkAccessViolation(scope, ptr)
                memory.writeUnsafe(ptr, rvalue, rhvType)
            } else {
                val lvalue = resolver.resolveLValue(stmt.lhv) ?: return@doWithState
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
//        val exception = resolver.resolve(stmt.value)?.asExpr(ctx.addressSort) ?: return }
        scope.doWithState {
            if (stmt.value is IlNull) {
                throwException(ctx.nullReferenceException, callStack.stackTrace(currentStatement).last())
            } else {
                throwException(stmt.value.type, callStack.stackTrace(currentStatement).last())
            }
        }

    }

    private fun visitRethrowStmt(scope: IlStepScope, stmt: IlRethrowStmt) = scope.doWithState {
        val lastEntry = exceptionsStack.removeLast()
        val ex = lastEntry.exception
        val updatedEx = IlMethodResult.Exception(ex.ref, ex.type, stmt.method, stmt)
        methodResult = updatedEx
        exceptionsStack.add(UnhandledExceptionEntry(updatedEx))
    }

    private fun visitEndFilterStmt(scope: IlStepScope, stmt: IlEndFilterStmt) {
        val exprResolver = mkExprResolver(scope)
        val filterExpr = stmt.value
        val filterValue = if (filterExpr.type != ctx.boolType) {
            exprResolver.resolve(IlConvCastExpr(ctx.boolType, filterExpr))
        } else {
            exprResolver.resolve(stmt.value)
        }
        filterValue ?: return
//        val booleanFilterValue = ctx.mkEq(filterValue, ctx.mkBv(1, ctx.int32sort))

        val enclosingFilter = stmt.enclosingFilter()
        val onTrue: IlState.() -> Unit = {
            val entry = exceptionsStack.removeLast()
            require(entry is UnhandledExceptionEntry)
            val observingFrame = memory.stack.observingFrame
            val handledExEntry = CaughtExceptionEntry(entry.exception, enclosingFilter, observingFrame)
            exceptionsStack.add(handledExEntry)
            val nextFinallyOrFault = findNextFinallyOrFault(enclosingFilter, handledExEntry.exception.stmt)
            if (nextFinallyOrFault != null) {
                val (handler, finallyFrameIdx) = nextFinallyOrFault
                // both statements are necessary. even if we have no frames to drop, we may
                // observe filter on another frame
                dropFramesAfterIndex(finallyFrameIdx)
                memory.stack.observingFrame = finallyFrameIdx
                newStmt(handler.hb)
            } else {
                newStmt(enclosingFilter.hb)
            }
        }
        val onFalse: IlState.() -> Unit = {
            val exception = exceptionsStack.last().exception
            methodResult = exception
            handleException(exception, enclosingFilter)
        }
        scope.fork(filterValue.asExpr(ctx.boolSort), onTrue, onFalse)
    }


    private fun visitEndFinallyOrFaultStmt(scope: IlStepScope, stmt: IlEhStmt) = scope.doWithState {
        assert(stmt is IlEndFaultStmt || stmt is IlEndFinallyStmt)
        // by building of TAC, we visit endFinallyStmt only if we execute finally block with
        // exception thrown, so we must execute all finally blocks before found catch
        val correspondingScope = stmt.method.scopes.find { it.he == stmt }
        // find rest scopes to execute
        val entry = exceptionsStack.last()
        val throwStmt = entry.exception.stmt
        require(entry is CaughtExceptionEntry) { "Exception is expected to be caught when executing finally" }
        val catch = entry.handler
        val finallyOrFault = findNextFinallyOrFault(catch, throwStmt, correspondingScope)
        if (finallyOrFault != null) {
            val (handler, handlerIdx) = finallyOrFault
            dropFramesAfterIndex(handlerIdx)
            newStmt(handler.hb)
        } else {
            dropFramesAfterIndex(entry.handlerFrameIdx)
            newStmt(catch.hb)
        }
    }

    private fun resolveVirtualCall(callStmt: IlVirtualCallStmt, scope: IlStepScope) {
        val typeSelector = IlFixedInheritorsNumberTypeSelector()
        resolveVirtualInvoke(callStmt, ctx, scope, typeSelector, ilOptions.forkOnRemainingTypes)
    }

    private fun IlStmt.next() : IlStmt = location.method.instList[location.index + 1]

    private fun mkExprResolver(scope: IlStepScope) =
        IlExprResolver(ctx, scope, ilOptions, strings, ::typesAllocator, ::mapMethodLocals)
}
