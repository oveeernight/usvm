package org.usvm.machine.interpreter

import io.ksmt.utils.asExpr
import org.jacodb.api.net.core.IlExprVisitor
import org.jacodb.api.net.ilinstances.*
import org.jacodb.api.net.ilinstances.impl.IlArrayType
import org.usvm.*
import org.usvm.api.allocateArray
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.array.length.UArrayLengthLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.IlContext
import org.usvm.machine.IlMachineOptions
import org.usvm.machine.USizeSort
import org.usvm.machine.state.insertConcreteCallStmt
import org.usvm.machine.state.throwException
import org.usvm.memory.ULValue
import org.usvm.memory.URegisterStackLValue

@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
class IlExprResolver(
    val ctx: IlContext,
    val scope: IlStepScope,
    val machineOptions: IlMachineOptions,
    getOrMkStringConst: MutableMap<String, UConcreteHeapRef>,
    getOrMkTypeRef: (IlType) -> UConcreteHeapRef,
    val mapMethodLocalToIdx: (IlMethod, IlLocal) -> Pair<Int, IlType>,
) : IlExprVisitor<UExpr<out USort>?> {

    private val constResolver = IlConstResolver(ctx, scope, getOrMkStringConst, getOrMkTypeRef)

    fun resolve(expr: IlExpr, type: IlType = expr.type) : UExpr<out USort>? = expr.accept(this)

    override fun visitIlNullConst(const: IlNull): UExpr<out USort> = constResolver.visitIlNullConst(const)
    override fun visitIlStringConst(const: IlStringConstant): UExpr<out USort> = constResolver.visitIlStringConst(const)
    override fun visitIlBoolConst(const: IlBoolConstant): UExpr<out USort> = constResolver.visitIlBoolConst(const)
    override fun visitIlCharConst(const: IlCharConstant): UExpr<out USort> = constResolver.visitIlCharConst(const)
    override fun visitIlInt8Const(const: IlInt8Constant): UExpr<out USort> = constResolver.visitIlInt8Const(const)
    override fun visitIlInt16Const(const: IlInt16Constant): UExpr<out USort> = constResolver.visitIlInt16Const(const)
    override fun visitIlInt32Const(const: IlInt32Constant): UExpr<out USort> = constResolver.visitIlInt32Const(const)
    override fun visitIlInt64Const(const: IlInt64Constant): UExpr<out USort> = constResolver.visitIlInt64Const(const)
    override fun visitIlUInt8Const(const: IlUInt8Constant): UExpr<out USort> = constResolver.visitIlUInt8Const(const)
    override fun visitIlUInt16Const(const: IlUInt16Constant): UExpr<out USort> = constResolver.visitIlUInt16Const(const)
    override fun visitIlUInt32Const(const: IlUInt32Constant): UExpr<out USort> = constResolver.visitIlUInt32Const(const)
    override fun visitIlUInt64Const(const: IlUInt64Constant): UExpr<out USort> = constResolver.visitIlUInt64Const(const)
    override fun visitIlFloatConst(const: IlFloatConstant): UExpr<out USort> = constResolver.visitIlFloatConst(const)
    override fun visitIlDoubleConst(const: IlDoubleConstant): UExpr<out USort> = constResolver.visitIlDoubleConst(const)
    override fun visitIlEnumConst(const: IlEnumConstant): UExpr<out USort> = constResolver.visitIlEnumConst(const)
    override fun visitIlArrayConst(const: IlArrayConstant): UExpr<out USort> = constResolver.visitIlArrayConst(const)
    override fun visitIlTypeRefConst(const: IlTypeRef): UExpr<out USort> = constResolver.visitIlTypeRefConst(const)

    override fun visitErrVar(expr: IlErrVar): UExpr<out USort>? {
        val key = localVarToLValue(expr)
        return scope.calcOnState { memory.read(key) }
    }

    override fun visitIlArg(expr: IlArgument): UExpr<out USort>?  {
        val key = localVarToLValue(expr)
        return scope.calcOnState { memory.read(key) }
    }

    override fun visitIlTempVar(expr: IlTempVar): UExpr<out USort> {
        val key = localVarToLValue(expr)
        return scope.calcOnState { memory.read(key) }
    }

    override fun visitIlLocalVar(expr: IlLocalVar): UExpr<out USort>? {
        val key = localVarToLValue(expr)
        return scope.calcOnState { memory.read(key) }
    }

    fun resolveLValue(expr: IlExpr) : ULValue<*, *>? {
        return when (expr) {
            is IlArrayAccess -> arrayAccessToLValue(expr)
            is IlFieldAccess -> fieldAccessToLValue(expr)
            is IlLocal -> localVarToLValue(expr)
            else -> error("resulveULValue: unexpected expr $expr")
        }

    }

    private fun localVarToLValue(local: IlLocal) : URegisterStackLValue<out USort> {
        val method = scope.calcOnState { callStack.lastMethod() }
        val (idx, type) = mapMethodLocalToIdx(method, local)
        val sort = ctx.typeToSort(type)
        return URegisterStackLValue<USort>(sort, idx)
    }

    private fun arrayAccessToLValue(expr: IlArrayAccess): UArrayIndexLValue<*, *, *>? = with(ctx) {
        val elementType = (expr.array.type as IlArrayType).elementType
        val arrayRef = resolve(expr.array)?.asExpr(addressSort) ?: return null
        checkNullPointer(arrayRef)

        val index = resolve(expr.index)?.asExpr(sizeSort) ?: return null

        val arrayType = ctx.arrayDescriptorOf(expr.array.type as IlArrayType)
        val len = UArrayLengthLValue(arrayRef, arrayType, sizeSort).let {
            scope.calcOnState { memory.read(it) }
        }
        checkArrayIndexBounds(index, len)
        val maxArrayLengthConstr = mkBvUnsignedLessExpr(len, machineOptions.maxArraySize.toBv(sizeSort))
        scope.assert(maxArrayLengthConstr)

        val lvalue = UArrayIndexLValue(typeToSort(elementType), arrayRef, index, arrayType)
        lvalue
    }

    private fun fieldAccessToLValue(expr: IlFieldAccess): UFieldLValue<*, *>? {
        val fieldIsStatic = expr.instance == null
        val field = expr.field
        if (!fieldIsStatic) {
            val instance = resolve(expr.instance!!)?.asExpr(ctx.addressSort) ?: return null
            checkNullPointer(instance)
            return UFieldLValue(ctx.typeToSort(field.fieldType), instance, field)
        }
        TODO("static fields")
    }

    private fun checkNullPointer(ref: UHeapRef) = with(ctx) {
        val constr = ref neq nullRef
        if (machineOptions.forkOnImplicitExceptions) {
            scope.fork(
                constr,
                blockOnFalseState = { throwException(nullReferenceException, callStack.stackTrace(currentStatement).last())
            })
        }
        else {
            // TODO handle exceptions, log ex
            scope.assert(constr) ?: error("checkNullPointer failed with forkInImplicitExceptions option turned off")
        }
    }

    private fun checkArrayIndexBounds(index: UExpr<USizeSort>, length: UExpr<USizeSort>) = with(ctx) {
        val inside = mkBvSignedLessExpr(index, length)
        if (machineOptions.forkOnImplicitExceptions) {
            scope.fork(
                inside,
                blockOnFalseState = { throwException(indexOutOfRangeException, callStack.stackTrace(currentStatement).last()) }
            )
        } else {
            // TODO handle exceptions, log ex
            scope.assert(inside) ?: error("checkArrayIndexBounds failed with forkOnImplicitExceptions option turned off")
        }
    }


    override fun visitIlArrayAccess(expr: IlArrayAccess): UExpr<out USort>? = scope.calcOnState {
        val key = arrayAccessToLValue(expr) ?: return@calcOnState null
        memory.read(key)
    }

    override fun visitIlFieldAccess(expr: IlFieldAccess): UExpr<out USort>? = scope.calcOnState {
        val key = fieldAccessToLValue(expr) ?: return@calcOnState null
        memory.read(key)
    }

    override fun visitIlArrayLength(expr: IlArrayLengthExpr): UExpr<out USort>? {
        val arrayRef = resolve(expr.array)?.asExpr(ctx.addressSort) ?: return null
        checkNullPointer(arrayRef)
        val arrayDesc = ctx.arrayDescriptorOf(expr.array.type as IlArrayType)
        val key = UArrayLengthLValue(arrayRef, arrayDesc, ctx.sizeSort)
        return scope.calcOnState { memory.read(key) }
    }

    override fun visitIlBinaryOp(expr: IlBinaryOp): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlBoxExpr(expr: IlBoxExpr): UExpr<out USort>? {
        val resolved = expr.operand.accept(this)

        TODO("Not yet implemented")
    }

    // TODO check instance can execute the method
    override fun visitIlCall(expr: IlCall): UExpr<out USort>? {
        val args = expr.args
        val method = expr.method
        val params = method.parameters
        val (instance, funArgs) = if (method.isStatic) {
            args[0] to args.subList(1, args.size)
        } else {
            null to args
        }
        return checkCall(
            instance,
            method,
            funArgs,
            params
        ) { resolvedArgs -> scope.doWithState { insertConcreteCallStmt(method, resolvedArgs) } }
    }

    private fun checkCall(
        instance: IlExpr?,
        method: IlMethod,
        args: List<IlExpr>,
        parameters: List<IlParameter>,
        onBeforeCall: IlStepScope.(List<UExpr<out USort>>) -> Unit
    ) : UExpr<out USort>? {
        if (instance != null) {
            val resolvedInstance = resolve(instance)?.asExpr(ctx.addressSort) ?: return null
            checkNullPointer(resolvedInstance)
        }

        val resolvedArgs = args.zip(parameters).map { (arg, param) -> resolve(arg, param.type) ?: return null }

        return resolveCall { onBeforeCall(resolvedArgs) }
    }

    private fun resolveCall(onBeforeCall: IlStepScope.() -> Unit): UExpr<out USort>? {
        val methodRes = scope.calcOnState { methodResult }
        return when (methodRes) {
            is IlMethodResult.BeforeCall -> {
                scope.onBeforeCall()
                null
            }

            is IlMethodResult.Success -> {
                scope.doWithState { methodResult = IlMethodResult.BeforeCall }
                methodRes.result
            }

            is IlMethodResult.Exception -> {
                error("Exceptions should be handled earlier")
            }
        }
    }

    override fun visitIlConvExpr(expr: IlConvCastExpr): UExpr<out USort>? = scope.calcOnState {
        val e = resolve(expr.operand)?.asExpr(ctx.addressSort) ?: return@calcOnState null
        val currType = expr.type
        val expectedType = expr.expectedType
        if (!ctx.typeSystem<IlType>().isSupertype(supertype = expectedType, type = currType)){
            checkClassCast(e, expectedType)
        }

        e
    }

    fun checkClassCast(ref: UHeapRef, type: IlType) = scope.calcOnState {
        val isSubtype = memory.types.evalIsSubtype(ref, type)
        if (machineOptions.forkOnImplicitExceptions) {
            scope.fork(
                isSubtype,
                blockOnFalseState = { throwException(ctx.invalidCastException, callStack.stackTrace(currentStatement).last()) }
            )
        }
        else {
            // TODO("fix when eh will be added)
            scope.assert(isSubtype) ?: error("checkClassCast failed with forkInImplicitExceptions option turned off")
        }
    }

    override fun visitIlFieldRefConst(const: IlFieldRef): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlIsInstExpr(expr: IlIsInstExpr): UExpr<out USort>? = scope.calcOnState {
        val instance = expr.operand.accept(this@IlExprResolver)?.asExpr(ctx.addressSort) ?: return@calcOnState null
        memory.types.evalIsSubtype(instance, expr.expectedType)
    }

    override fun visitIlManagedDerefExpr(expr: IlManagedDerefExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlManagedRefExpr(expr: IlManagedRefExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlMethodRefConst(const: IlMethodRef): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlNewArrayExpr(expr: IlNewArrayExpr): UExpr<out USort>? = scope.calcOnState {
        val arrayType = expr.elementType
        val size = expr.size.accept(this@IlExprResolver)?.asExpr(ctx.sizeSort) ?: return@calcOnState null
        memory.allocateArray(arrayType, ctx.sizeSort, size)
    }

    override fun visitIlNewExpr(expr: IlNewExpr): UExpr<out USort>? = scope.calcOnState {
        memory.allocConcrete(expr.type)
    }

    override fun visitIlSizeOfExpr(expr: IlSizeOfExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlStackAllocExpr(expr: IlStackAllocExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlUnaryOp(expr: IlUnaryOp): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlUnboxExpr(expr: IlUnboxExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlUnmanagedDerefExpr(expr: IlUnmanagedDerefExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlUnmanagedRefExpr(expr: IlUnmanagedRefExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }
}
