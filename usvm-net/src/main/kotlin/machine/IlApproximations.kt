package org.usvm.machine

import io.ksmt.utils.asExpr
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.api.memcpy
import org.usvm.collection.array.length.UArrayLengthLValue
import org.usvm.machine.interpreter.IlExprResolver
import org.usvm.machine.interpreter.IlStepScope
import org.usvm.machine.interpreter.MethodCall
import org.usvm.machine.state.IlState
import org.usvm.machine.state.lastStackTraceFrame
import org.usvm.machine.state.skipMethodInvokeWithValue
import org.usvm.machine.state.throwException

class IlApproximationsResolver(val ctx: IlContext) {
    private var currentScope: IlStepScope? = null
    private var currentExprResolver: IlExprResolver? = null

    private val scope : IlStepScope
        get() = checkNotNull(currentScope)
    private val exprResolver: IlExprResolver
        get() = checkNotNull(currentExprResolver)
    fun approximate(call: MethodCall, stepScope: IlStepScope, exprResolver: IlExprResolver): Boolean = try {
        currentScope = stepScope
        currentExprResolver = exprResolver
        approximate(call)
    } finally {
        currentExprResolver = null
        currentScope = null
    }

    private fun approximate(call: MethodCall): Boolean {
        val m = call.method
        if (m.isStatic && m.name == "Copy" && m.declaringType.fullname == "System.Array" ) {
            val args = call.args
            val srcRef = args[0].asExpr(ctx.addressSort)
            val srcIdx = args[1].asExpr(ctx.sizeSort)
            val dstRef = args[2].asExpr(ctx.addressSort)
            val dstIdx = args[3].asExpr(ctx.sizeSort)
            val len = args[4].asExpr(ctx.sizeSort)
            exprResolver.resolveArrayCopy(call, srcRef, srcIdx, dstRef, dstIdx, len)
            return true
        }
        return false
    }

    private fun MutableMap<String, (MethodCall) -> UExpr<out USort>?>.dispatchUsvmApiMethod(
        fullName: String,
        model: (MethodCall) -> UExpr<out USort>
    ) {
        this[fullName] = model
    }

    private fun IlExprResolver.resolveArrayCopy(
        methodCall: MethodCall,
        srcRef: UHeapRef,
        srcIdx: UExpr<UBv32Sort>,
        dstRef: UHeapRef,
        dstIdx: UExpr<UBv32Sort>,
        length: UExpr<UBv32Sort>
    ) {
        val possibleElementTypes = ctx.primitiveTypes + ctx.objectType
        val possibleArrayTypes = possibleElementTypes.map { ctx.arrayTypeOf(it) }
        val blocks = mutableListOf<Pair<UBoolExpr, (IlState) -> Unit>>()

        with(ctx) {
            val argsNotNull = !mkHeapRefEq(srcRef, nullRef) and !mkHeapRefEq(dstRef, nullRef)

            blocks += !argsNotNull to { state: IlState ->
                state.throwException(
                    argumentNullException,
                    state.lastStackTraceFrame
                )
            }
        }

        possibleElementTypes.forEach { type ->
            copyForPossibleElementType(
                methodCall,
                type, blocks, srcRef, srcIdx, dstRef, dstIdx, length
            )
        }

        val arrayTypeConstraints = possibleArrayTypes.map {
            scope.calcOnState {
                ctx.mkAnd(
                    memory.types.evalIsSubtype(srcRef, it),
                    memory.types.evalIsSubtype(dstRef, it)
                )
            }
        }

        val unknownArrayType = ctx.mkAnd(arrayTypeConstraints.map { ctx.mkNot(it) })
        val onUnknownArrayType: IlState.() -> Unit = {
            throwException(ctx.arrayTypeMismatchException)
        }

        blocks += unknownArrayType to onUnknownArrayType

        scope.forkMulti(blocks)
    }

    private fun copyForPossibleElementType(
        methodCall: MethodCall,
        elementType: IlType,
        blocks: MutableList<Pair<UBoolExpr, (IlState) -> Unit>>,
        srcRef: UHeapRef,
        srcIdx: UExpr<UBv32Sort>,
        dstRef: UHeapRef,
        dstIdx: UExpr<UBv32Sort>,
        length: UExpr<UBv32Sort>
    ) = with(ctx) {
        val sort = ctx.typeToSort(elementType)
        val typeConstraint = scope.calcOnState {
            val arrayType = arrayTypeOf(elementType)
            memory.types.evalIsSubtype(srcRef, arrayType) and memory.types.evalIsSubtype(dstRef, arrayType)
        }


        val argsNotNull = !mkHeapRefEq(srcRef, nullRef) and !mkHeapRefEq(dstRef, nullRef)

        // TODO lower bounds
        val indicesAreValid = mkAnd(
            mkBvSignedLessOrEqualExpr(mkBv(0), srcIdx),
            mkBvSignedLessOrEqualExpr(mkBv(0), dstIdx),
            mkBvSignedLessOrEqualExpr(mkBv(0), length),
        )

        blocks += typeConstraint and argsNotNull and !indicesAreValid to { state: IlState ->
            state.throwException(argumentOutOfRangeException, state.lastStackTraceFrame)
        }

        val srcLenKey = UArrayLengthLValue(srcRef, elementType, bv32Sort)
        val srcLen = scope.calcOnState { memory.read(srcLenKey) }
        val dstLenKey = UArrayLengthLValue(dstRef, elementType, bv32Sort)
        val dstLen = scope.calcOnState { memory.read(dstLenKey) }

        val canAccessAllIndices =
            mkBvSignedLessOrEqualExpr(mkBvAddExpr(srcIdx, length), srcLen) and
                    mkBvSignedLessOrEqualExpr(mkBvAddExpr(dstIdx, length), dstLen)

        blocks += mkAnd(typeConstraint, argsNotNull, indicesAreValid, !canAccessAllIndices) to { state: IlState ->
            state.throwException(argumentException, state.lastStackTraceFrame)
        }


        val successConstraint = mkAnd(typeConstraint, argsNotNull, indicesAreValid, canAccessAllIndices)
        val onSuccess: IlState.() -> Unit = {
            memory.memcpy(srcRef, dstRef, elementType, sort, srcIdx, dstIdx, length)
            skipMethodInvokeWithValue(methodCall, ctx.void)
        }

        blocks += successConstraint to onSuccess

//        val onInvalidArguments: IlState.() -> Unit = {
//            throwException(ctx.argumentOutOfRangeException, lastStackTraceFrame)
//        }
//
//        blocks += mkNot(argumentsAreOk) to onInvalidArguments
//
//        val canNotAccessAllIndices = argumentsAreOk and mkNot(canAccessAllIndices)
//
//        val onCanNotAccessIndices: IlState.() -> Unit = {
//            throwException(ctx.argumentException, lastStackTraceFrame)
//        }
//
//        blocks += canNotAccessAllIndices to onCanNotAccessIndices
//
//
//        val onInvalidTypes: IlState.() -> Unit = {
//            throwException(ctx.arrayTypeMismatchException, lastStackTraceFrame)
//        }
//
//        val invalidTypesCondition = mkAnd(argumentsAreOk, canNotAccessAllIndices, mkNot(typeConstraint))
//
//        blocks += invalidTypesCondition to onInvalidTypes

    }
}
