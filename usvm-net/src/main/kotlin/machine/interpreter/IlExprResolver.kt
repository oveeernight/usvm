package org.usvm.machine.interpreter

import io.ksmt.utils.asExpr
import io.ksmt.utils.cast
import org.jacodb.api.net.core.IlExprVisitor
import org.jacodb.api.net.ilinstances.*
import org.jacodb.api.net.ilinstances.impl.*
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.array.length.UArrayLengthLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.collection.field.UInputFieldReading
import org.usvm.machine.*
import org.usvm.machine.state.*
import org.usvm.machine.state.boxed.IlBoxedLocationLValue
import org.usvm.memory.ULValue
import org.usvm.utils.logAssertFailure

@Suppress("UNUSED_PARAMETER")
class IlExprResolver(
    private val ctx: IlContext,
    private val scope: IlStepScope,
    private val machineOptions: IlMachineOptions,
    getOrMkStringConst: MutableMap<String, UConcreteHeapRef>,
    getOrMkTypeRef: (IlType) -> UConcreteHeapRef,
    val mapMethodLocalToIdx: (IlMethod, IlLocal) -> Int,
) : IlExprVisitor<UExpr<out USort>?> {

    private val valueSampler by lazy { ctx.mkUValueSampler() }
    private val constResolver = IlConstResolver(ctx, scope, getOrMkStringConst, getOrMkTypeRef)

    fun resolve(expr: IlExpr, type: IlType = expr.type) : UExpr<out USort>? {
        val resolved = expr.accept(this) ?: return null
        ensureExprCorrectness(resolved, type) ?: return null
        return resolved
    }

    private fun ensureExprCorrectness(expr: UExpr<out USort>, type: IlType): Unit? {
        if (ctx.isPrimitiveType(type))
            return Unit

        return ensureStaticFieldsInitialized(type) { }
    }

    private inline fun <T> ensureStaticFieldsInitialized(type: IlType, body : () -> T): T? {
        val staticCtor = type.methods.find { it.name == ".cctor" }
        if (staticCtor == null) {
            return body()
        }
        val staticFieldsAreInitialized = scope.calcOnState { typeIsInitialized(type) }
        if (staticFieldsAreInitialized) {
//            scope.doWithState {
//                val mr = methodResult
//                if (mr is IlMethodResult.Success && mr.method == staticCtor) {
//                    // need to mutate primitives to symbolicValues?
//                }
//            }
            return body()
        }

        scope.doWithState {
            markTypeInitialized(type)
            insertConcreteCallStmt(staticCtor, args = emptyList())
        }

        return null
    }

    private fun IlState.markTypeInitialized(type: IlType) {
        val lvalue = IlStaticFieldLValue(ctx.staticFieldsInitializedFlag, ctx.boolSort)
        memory.write(lvalue, ctx.trueExpr, guard = ctx.trueExpr)
    }

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
        return scope.calcOnState {
            val exRef = exceptionsStack.last().exception.ref
            memory.write(key, exRef)
            memory.read(key) }
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
            is IlManagedRefExpr -> resolveLValue(expr.value)
            is IlManagedDerefExpr -> resolveLValue(expr.value)
            else -> error("resolveULValue: unexpected expr $expr")
        }
    }

    private fun localVarToLValue(local: IlLocal) : IlRegisterStackLValue<out USort> {
        val (method, frameIdx) = scope.calcOnState { callStack.lastMethod() to memory.stack.observingFrame }
        val regIdx = mapMethodLocalToIdx(method, local)
        val sort = ctx.typeToSort(local.type)
        return IlRegisterStackLValue(sort, frameIdx, regIdx)
    }

    private fun arrayAccessToLValue(expr: IlArrayAccess): UArrayIndexLValue<*, *, *>? = with(ctx) {
        val elementType = (expr.array.type as IlArrayType).elementType
        val arrayRef = resolve(expr.array)?.asExpr(addressSort) ?: return null
        checkNullPointer(arrayRef) ?: return null

        val index = resolve(expr.index)?.asExpr(sizeSort) ?: return null

        val arrayDesc = ctx.arrayDescriptorOf(expr.array.type as IlArrayType)
        val len = UArrayLengthLValue(arrayRef, arrayDesc, sizeSort).let {
            scope.calcOnState { memory.read(it) }
        }

//        scope.fork(mkBvSignedLessOrEqualExpr(mkBv(0), len),
//            blockOnFalseState = {
//                throwException(overflowException, lastStackTraceFrame)
//            }) ?: return@with null


        val maxArrayLengthConstr = mkBvUnsignedLessExpr(len, machineOptions.maxArraySize.toBv(sizeSort))
        scope.assert(maxArrayLengthConstr).logAssertFailure { "IlExprResolver: array length max" }

        checkArrayIndexBounds(index, len) ?: return null


        val lvalue = UArrayIndexLValue(typeToSort(elementType), arrayRef, index, arrayDesc)
        lvalue
    }

    private fun fieldAccessToLValue(expr: IlFieldAccess): ULValue<*, *>? = scope.calcOnState {
        val fieldIsStatic = expr.instance == null
        val field = expr.field
        if (!fieldIsStatic) {
            val instance = resolveInstance(expr.instance!!) ?: return@calcOnState null
            val key = UFieldLValue(ctx.typeToSort(field.fieldType), instance, field)
            if (field.declaringType is IlStructType && instance is USymbol<UAddressSort>) {
                setStructFieldsDefaultValues(ctx.nullRef, field.declaringType as IlStructType)
                assertStructLocation(instance)
            }
            if (field.declaringType !is IlStructType) checkNullPointer(instance) ?: return@calcOnState null
            key
        } else {
            ensureStaticFieldsInitialized(field.declaringType) {
                IlStaticFieldLValue(field, ctx.typeToSort(expr.type))
            }
        }
    }

    private fun assertStructLocation(structSymbol: USymbol<UAddressSort>) {
        when (structSymbol) {
            is UInputFieldReading<*, *> -> {
                val structLocation = structSymbol.address
                val syntheticLocationKey = UFieldLValue(ctx.addressSort, structSymbol, ctx.syntheticStructLocationField)
                val syntheticLocation = scope.calcOnState { memory.read(syntheticLocationKey) }
                val aliasBanCondition = ctx.mkHeapRefEq(syntheticLocation, structLocation)
                scope.fork(aliasBanCondition,
                    blockOnTrueState = {},
                    blockOnFalseState = { criticalErrorOccurred = true })
            }
            else -> TODO()
        }
    }

    fun throwException(type: IlType) = scope.doWithState {
            val frame = callStack.stackTrace(currentStatement).last()
            throwException(type, frame)
        }

    fun checkNullPointer(ref: UHeapRef, exception: IlType? = null): Unit? = with(ctx) {
//        if (type.baseType == ctx.valueType || type is IlPointerType && type.targetType.baseType == ctx.valueType) return@with
        val toThrow = exception ?: ctx.nullReferenceException
        val constr = !ctx.mkHeapRefEq(ref, nullRef)
        return if (machineOptions.forkOnImplicitExceptions) {
            scope.fork(
                constr,
                blockOnFalseState = { throwException(toThrow, lastStackTraceFrame)
            })
        }
        else {
            // TODO handle exceptions, log ex
            scope.assert(constr) ?: error("checkNullPointer failed with forkInImplicitExceptions option turned off")
        }
    }

    private fun checkArrayIndexBounds(index: UExpr<USizeSort>, length: UExpr<USizeSort>): Unit? = with(ctx) {
        // TODO lower bounds
        val inside = mkBvSignedLessOrEqualExpr(mkBv(0), index) and mkBvSignedLessExpr(index, length)
        return if (machineOptions.forkOnImplicitExceptions) {
            scope.fork(
                inside,
                blockOnFalseState = { throwException(indexOutOfRangeException, lastStackTraceFrame) }
            )
        } else {
            // TODO handle exceptions, log ex
            scope.assert(inside) ?: error("checkArrayIndexBounds failed with forkOnImplicitExceptions option turned off")
        }
    }


    override fun visitIlArrayAccess(expr: IlArrayAccess): UExpr<out USort>? = scope.calcOnState {
        val key = arrayAccessToLValue(expr) ?: return@calcOnState null
        val reading = memory.read(key)
        if (assertIsSubtype(reading, expr.type)) reading else null
    }

    private fun assertIsSubtype(expr: UExpr<out USort>, type: IlType): Boolean {
        if (!ctx.isPrimitiveType(type)) {
            val ref = expr.asExpr(ctx.addressSort)
            val isExpr = scope.calcOnState { memory.types.evalIsSubtype(ref, type) }
            scope.assert(isExpr)
                .logAssertFailure {  "IlExprResolver: subtype constrain on $expr with $type is unsatisfiable" }
                ?: return false
        }
        return true
    }

    override fun visitIlFieldAccess(expr: IlFieldAccess): UExpr<out USort>? = scope.calcOnState {
        val key = fieldAccessToLValue(expr) ?: return@calcOnState null
        memory.read(key)
    }

    override fun visitIlArrayLength(expr: IlArrayLengthExpr): UExpr<out USort>? {
        val arrayRef = resolve(expr.array)?.asExpr(ctx.addressSort) ?: return null
        checkNullPointer(arrayRef) ?: return null
        val arrayDesc = ctx.arrayDescriptorOf(expr.array.type as IlArrayType)
        val key = UArrayLengthLValue(arrayRef, arrayDesc, ctx.sizeSort)
        return scope.calcOnState { memory.read(key) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitIlBinaryOp(expr: IlBinaryOp): UExpr<out USort>? = with(ctx) {
        val operator = IlBinaryOperator.resolve(expr)
        return resolveAfterResolved(expr.lhs, expr.rhs) { lhs, rhs ->
            if (lhs.sort == addressSort && rhs.sort == addressSort) {
                when (operator) {
                    is IlBinaryOperator.CEq -> mkHeapRefEq(lhs.cast(), rhs.cast())
                    is IlBinaryOperator.CNe -> mkNot(mkHeapRefEq(lhs.cast(), rhs.cast()))
                    else -> operator(lhs, rhs)
                }
            } else {
                val result = operator(lhs, rhs)
                val lhsSort = lhs.sort
                if ((operator is IlBinaryOperator.Div || operator is IlBinaryOperator.Rem) && lhsSort is UBvSort) {
                    val rhsIsNotZero = mkNot(mkEq(ctx.mkBv(0, lhsSort), rhs.asExpr(lhsSort)))
                    scope.fork(rhsIsNotZero,
                        blockOnFalseState = {
                            throwException(
                                divideByZeroException,
                                lastStackTraceFrame
                            )
                        }) ?: return@with null
                }
                if (lhs.sort is UBvSort && expr.isChecked && IlBinaryOperator.overflowIsPossible(operator)) {
                    val noOverflow = operator.bvNoOverflowCondition(
                        ctx,
                        lhs as UExpr<UBvSort>,
                        rhs as UExpr<UBvSort>,
                        !expr.isUnsigned
                    )
                    scope.fork(
                        noOverflow,
                        blockOnFalseState = {
                            throwException(
                                ctx.overflowException,
                                lastStackTraceFrame
                            )
                        }) ?: return null
                }
                result
            }
        }
    }

    override fun visitIlBoxExpr(expr: IlBoxExpr): UExpr<out USort>? {
        val operand = resolve (expr.operand) ?: return null
        return boxExpr(operand, expr.operand.type)
    }

    private fun boxExpr(expr: UExpr<out USort>, exprType: IlType): UHeapRef = scope.calcOnState {
        // TODO handle someday
//        assert(!exprType.isGenericType)
        if (exprType is IlReferenceType) return@calcOnState expr.asExpr(ctx.addressSort)
        require(exprType is IlValueType)
        when {
            exprType.nullable -> {
                val hasValueField = exprType.fields.first { f -> f.name == "hasValue" }
                val valueField = exprType.fields.first { f -> f.name == "value" }
                val hasValue =
                    UFieldLValue(ctx.boolSort, expr.asExpr(ctx.addressSort), hasValueField).let { memory.read(it) }
                val value = UFieldLValue(
                    ctx.typeToSort(valueField.fieldType),
                    expr.asExpr(ctx.addressSort),
                    valueField
                ).let { memory.read(it) }
                val res = ctx.mkIte(hasValue, boxExpr(value, valueField.fieldType), ctx.nullRef)
                res
            }

            exprType is IlStructType -> {
                val structRef = expr.asExpr(ctx.addressSort)
                copyStruct(structRef, exprType)
            }

            else -> {
                val freshAddress = memory.allocConcrete(exprType)
                val key = IlBoxedLocationLValue(ctx.typeToSort(exprType), freshAddress)
                memory.write(key, expr)
                freshAddress
            }
        }
    }


    // TODO check instance can execute the method
    override fun visitIlCall(expr: IlCall): UExpr<out USort>? {
        val args = expr.args
        val method = expr.method
        val params = method.parameters
        val instance = if (method.isStatic) null else args[0]
        if (method.isVirtual) {
            return checkCall(instance, method, args, params) { resolvedArgs ->
                scope.doWithState { insertVirtualCallStmt(method, resolvedArgs) }
            }
        }
        return checkCall(
            instance,
            method,
            args,
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
            val resolvedInstance = resolveInstance(instance) ?: return null
            if (instance !is IlManagedRefExpr) checkNullPointer(resolvedInstance) ?: return null
        }

        val resolvedArgs = args.zip(parameters).map { (arg, param) ->
            val resolved = resolve(arg, param.type) ?: return null
            val argType = arg.type
            if (argType is IlStructType) {
                val structRef = resolved.asExpr(ctx.addressSort)
                scope.calcOnState { copyStruct(structRef, argType) }
            } else resolved
        }

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

    @Suppress("UNCHECKED_CAST")
    override fun visitIlConvExpr(expr: IlConvCastExpr): UExpr<out USort>? = scope.calcOnState {
        if (expr.operand.type == expr.expectedType) return@calcOnState resolve(expr.operand)
        val currType = expr.operand.type
        val expectedType = expr.expectedType
        resolveAfterResolved(expr.operand) { operand ->
            if (isPtrType(expectedType)) {
                val pointedType = extractPointedType(expectedType)
                when  {
                    operand is IlPtr -> {
                        ctx.mkPtr(operand.base, operand.baseType, operand.offset, pointedType)
                    }
                    operand is IlManagedRef<*> -> {
                        val (base, offset, locationType) = operand.toPtrInfo()
                        offset as UExpr<UBvSort>
                        ctx.mkPtr(base, operand.targetType, offset, pointedType)
                    }

                    operand.sort == ctx.addressSort -> {
                        require(pointedType is IlStructType)
                        val ulValue = resolveLValue(expr.operand) ?: return@calcOnState null
                        val offset: UExpr<UBvSort> = ctx.mkBv(0, ctx.bv32Sort)
                        ctx.mkPtr(ulValue, expr.operand.type, offset, pointedType)
                    }

                    operand.sort is UBvSort -> {
                        operand as UExpr<UBvSort>
                        ctx.mkDetachedPtr(operand, pointedType )
                    }

                    else -> error("Unexpected operand $operand of pointer cast")
                }
            }
            else {
                when (expr.type) {
                    is IlPrimitiveType -> resolvePrimitiveCast(operand, currType, expectedType)
                    else -> {
                        val instance = operand.let {
                            if (it is IlManagedRef<*>) memory.read(it.memoryKey)
                            else it
                        }.asExpr(ctx.addressSort)
                        if (instance == ctx.nullRef) {
                            return@calcOnState instance
                        }
                        if (!ctx.typeSystem<IlType>().isSupertype(supertype = expectedType, type = currType)) {
                            checkClassCast(instance, expectedType) ?: return@calcOnState null
                        }
                        instance
                    }
                }
            }
        }
    }

    private fun isPtrType(type: IlType): Boolean {
        return (type is IlPointerType || type.name == "UIntPtr" || type.name == "IntPtr")
    }

    private fun extractPointedType(type: IlType): IlType =
        when {
            type.name == "UIntPtr" -> ctx.uint32Type
            type.name == "IntPtr" -> ctx.int32Type
            type is IlPointerType -> type.targetType
            else -> TODO()
        }

    private fun resolvePrimitiveCast(
        expr: UExpr<out USort>,
        currType: IlType,
        expectedType: IlType
    ): UExpr<out USort> = with(ctx) {
        when (expectedType) {
            boolType -> IlUnaryOperator.CastToBool(expr)
            int8Type -> IlUnaryOperator.CastToInt8(expr)
            uint8Type -> IlUnaryOperator.CastToUInt8(expr)
            int16Type -> IlUnaryOperator.CastToInt16(expr)
            uint16Type -> IlUnaryOperator.CastToUInt16(expr)
            int32Type -> IlUnaryOperator.CastToInt32(expr)
            uint32Type -> IlUnaryOperator.CastToUInt32(expr)
            int64Type -> IlUnaryOperator.CastToInt64(expr)
            uint64Type -> IlUnaryOperator.CastToUInt64(expr)
            floatType -> IlUnaryOperator.CastToFloat(expr)
            doubleType -> IlUnaryOperator.CastToDouble(expr)
            else -> error("resolvePrimitiveCast: unexpected type $expectedType")
        }
    }

    private fun checkClassCast(ref: UHeapRef, type: IlType) = scope.calcOnState {
        val isSubtype = memory.types.evalIsSubtype(ref, type)
        if (machineOptions.forkOnImplicitExceptions) {
            scope.fork(
                isSubtype,
                blockOnFalseState = { throwException(ctx.invalidCastException, lastStackTraceFrame) }
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
        val inst = resolve(expr.operand)?.asExpr(ctx.addressSort) ?: return@calcOnState null
        val expectedType = expr.expectedType
        val isInstCond = memory.types.evalIsSubtype(inst, expectedType)
        ctx.mkIte(isInstCond, inst, ctx.nullRef)
    }

    override fun visitIlManagedDerefExpr(expr: IlManagedDerefExpr): UExpr<out USort>? {
        val ref = resolve(expr.value) ?: return null
        ref as IlManagedRef<*>
        return scope.calcOnState {
            memory.read(ref.memoryKey)
        }
    }

    override fun visitIlManagedRefExpr(expr: IlManagedRefExpr): IlManagedRef<out USort>? {
        val key = resolveLValue(expr.value) ?: return null
        val type = expr.value.type
        return IlManagedRef(ctx, type, key)
    }

    override fun visitIlMethodRefConst(const: IlMethodRef): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlNewArrayExpr(expr: IlNewArrayExpr): UExpr<out USort>? = scope.calcOnState {
        val ref = memory.allocConcrete(expr.type)
        val size = resolve(expr.size)?.asExpr(ctx.sizeSort) ?: return@calcOnState null
        val arrayDesc = ctx.arrayDescriptorOf(expr.type)
        memory.write(UArrayLengthLValue(ref, arrayDesc, ctx.sizeSort), size)
        memory.types.allocate(ref.address, expr.type)
        ref
    }

    override fun visitIlNewExpr(expr: IlNewExpr): UExpr<out USort> = scope.calcOnState {
        val type = expr.type
        val ref = memory.allocConcrete(type)
        if (type is IlStructType) {
            setStructFieldsDefaultValues(ref, type)
        }
//        for (sf in type.fields.filter { it.fieldType is IlStructType }) {
//            val structRef = memory.allocConcrete(sf.fieldType)
//            val structFieldKey = UFieldLValue(ctx.addressSort, ref, sf)
//            memory.write(structFieldKey, structRef)
//            setStructFieldsDefaultValues(structRef, sf.fieldType.cast())
//        }
        ref
    }

    private fun setStructFieldsDefaultValues(structRef: UHeapRef, type: IlStructType): Unit = scope.calcOnState {
        val fields = type.fields
        fields.forEach { field ->
            val fieldType = field.fieldType
            val fieldSort = ctx.typeToSort(fieldType)
            val fieldKey = UFieldLValue(fieldSort, structRef, field)
            val fieldValue = if (fieldType is IlStructType) {
                val newRef = memory.allocConcrete(fieldType)
                setStructFieldsDefaultValues(newRef, fieldType)
                newRef
            } else fieldSort.sampleUValue()
            memory.write(fieldKey, fieldValue.cast(), ctx.trueExpr)
        }
    }

    override fun visitIlSizeOfExpr(expr: IlSizeOfExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlStackAllocExpr(expr: IlStackAllocExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    override fun visitIlUnaryOp(expr: IlUnaryOp): UExpr<out USort>? {
        val operator = IlUnaryOperator.resolve(expr)
        return resolveAfterResolved(expr.operand) { operand ->
            operator(operand)

        }
    }

    // actually, it is UnboxAny
    override fun visitIlUnboxExpr(expr: IlUnboxExpr): UExpr<out USort>? = with(ctx) {
        val instance = resolve(expr.operand)?.asExpr(addressSort) ?: return@with null
        val expectedType = expr.expectedType
        if (expectedType is IlReferenceType) {
            val isExpr = scope.calcOnState { memory.types.evalIsSubtype(instance, expectedType) }
            scope.fork(
                isExpr,
                blockOnFalseState = { throwException(ctx.invalidCastException, lastStackTraceFrame) }
            ) ?: return null
            return instance
        }
        require(expectedType is IlValueType)
        val instanceIsNull = mkHeapRefEq(instance, nullRef)
        val forkCases = mutableListOf<Pair<UBoolExpr, IlState.() -> Unit>>()
        if (!expectedType.nullable) {
            forkCases += instanceIsNull to {
                throwException(
                    nullReferenceException,
                    lastStackTraceFrame
                )
            }
            val instanceIsNotNull = !instanceIsNull
            val isSubtype = mkIsSubtypeExpr(instance, expectedType)
            val castIsValid = mkAnd(instanceIsNotNull, isSubtype)
            val reading = IlBoxedLocationLValue(typeToSort(expectedType), instance).let {
                scope.calcOnState { memory.read(it)}
            }
            forkCases += castIsValid to { }
            val castIsInvalid = mkAnd(instanceIsNotNull, !isSubtype)
            forkCases += castIsInvalid to {
                throwException(
                    invalidCastException,
                    lastStackTraceFrame
                )
            }
            reading
        } else {
            scope.calcOnState {
                val hasValueField = expectedType.fields.first { it.name == "hasValue" }
                val valueField = expectedType.fields.first { it.name == "value" }
                val nullableExprNullCase = memory.allocConcrete(expectedType)
                UFieldLValue(boolSort, nullableExprNullCase, hasValueField).let {
                    memory.write(it, falseExpr)
                }

                if (instance == ctx.nullRef) {
                    return@calcOnState nullableExprNullCase
                }

                val nullableExprNonNullCase = memory.allocConcrete(expectedType)
                UFieldLValue(boolSort, nullableExprNonNullCase, hasValueField).let {
                    memory.write(it, trueExpr)
                }
                // TODO check whether its struct
                val underlyingTypeSort = typeToSort(valueField.fieldType)
                val value = IlBoxedLocationLValue(underlyingTypeSort, instance).let { memory.read(it) }

                UFieldLValue(underlyingTypeSort, nullableExprNonNullCase, valueField).let { memory.write(it, value) }

                val result = mkIte(instanceIsNull, nullableExprNullCase, nullableExprNonNullCase)
                result
            }
        }
    }

    override fun visitIlUnmanagedDerefExpr(expr: IlUnmanagedDerefExpr): UExpr<out USort>? {
        // visitAssignStmt catches cases when managed deref is a key, here it is a value
        val ptr = resolve(expr.value)
        ptr as IlPtr
        return scope.calcOnState {
            memory.readUnsafe(ptr)
        }
    }

    override fun visitIlUnmanagedRefExpr(expr: IlUnmanagedRefExpr): UExpr<out USort>? {
        TODO("Not yet implemented")
    }

    private fun resolveInstance(instance: IlExpr) : UHeapRef? = resolve(instance).let {
        when (it) {
            is IlManagedRef<*> -> scope.calcOnState { memory.read(it.memoryKey) }
            is IlPtr -> scope.calcOnState { memory.read(it.base!!) }
            else -> it
        }
    }?.asExpr(ctx.addressSort)

    private inline fun <T> resolveAfterResolved(expr: IlExpr, block: (UExpr<out USort>) -> T): T? {
        val resolved = resolve(expr) ?: return null
        return block(resolved)
    }

    private inline fun <T> resolveAfterResolved(
        expr1: IlExpr,
        expr2: IlExpr,
        block: (UExpr<out USort>, UExpr<out USort>) -> T
    ): T? {
        val resolved1 = resolve(expr1) ?: return null
        val resolved2 = resolve(expr2) ?: return null

        return block(resolved1, resolved2)
    }
}
