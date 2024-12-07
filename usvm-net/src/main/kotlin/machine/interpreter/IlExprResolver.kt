package org.usvm.machine.interpreter

import io.ksmt.utils.asExpr
import org.example.ilinstances.IlMethod
import org.example.ilinstances.IlType
import org.jacodb.api.net.core.IlExprVisitor
import org.jacodb.api.net.ilinstances.*
import org.usvm.*
import org.usvm.api.allocateArray
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.array.length.UArrayLengthLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.IlContext
import org.usvm.machine.IlMachineOptions
import org.usvm.machine.USizeSort
import org.usvm.machine.state.throwException
import org.usvm.memory.ULValue
import org.usvm.memory.URegisterStackLValue

class IlExprResolver(
    val ctx: IlContext,
    val scope: IlStepScope,
    val machineOptions: IlMachineOptions,
    getOrMkStringConst: (String) -> UConcreteHeapRef,
    getOrMkTypeRef: (IlType) -> UConcreteHeapRef,
    val mapMethodLocalToIdx: (IlMethod, IlLocal) -> Pair<Int, IlType>,
) : IlExprVisitor<UExpr<out USort>> {

    private val constResolver = IlConstResolver(ctx, scope, getOrMkStringConst, getOrMkTypeRef)

    fun resolve(expr: IlExpr, type: IlType = ctx.mockType) : UExpr<out USort> = expr.accept(this)

    override fun visitIlNullConst(const: IlNull): UExpr<out USort> = constResolver.visitIlNullConst(const)
    override fun visitIlStringConst(const: IlStringConst): UExpr<out USort> = constResolver.visitIlStringConst(const)
    override fun visitIlBoolConst(const: IlBoolConst): UExpr<out USort> = constResolver.visitIlBoolConst(const)
    override fun visitIlCharConst(const: IlCharConst): UExpr<out USort> = constResolver.visitIlCharConst(const)
    override fun visitIlInt8Const(const: IlInt8Const): UExpr<out USort> = constResolver.visitIlInt8Const(const)
    override fun visitIlInt16Const(const: IlInt16Const): UExpr<out USort> = constResolver.visitIlInt16Const(const)
    override fun visitIlInt32Const(const: IlInt32Const): UExpr<out USort> = constResolver.visitIlInt32Const(const)
    override fun visitIlInt64Const(const: IlInt64Const): UExpr<out USort> = constResolver.visitIlInt64Const(const)
    override fun visitIlUInt8Const(const: IlUInt8Const): UExpr<out USort> = constResolver.visitIlUInt8Const(const)
    override fun visitIlUInt16Const(const: IlUInt16Const): UExpr<out USort> = constResolver.visitIlUInt16Const(const)
    override fun visitIlUInt32Const(const: IlUInt32Const): UExpr<out USort> = constResolver.visitIlUInt32Const(const)
    override fun visitIlUInt64Const(const: IlUInt64Const): UExpr<out USort> = constResolver.visitIlUInt64Const(const)
    override fun visitIlFloatConst(const: IlFloatConst): UExpr<out USort> = constResolver.visitIlFloatConst(const)
    override fun visitIlDoubleConst(const: IlDoubleConst): UExpr<out USort> = constResolver.visitIlDoubleConst(const)
    override fun visitIlEnumConst(const: IlEnumConst): UExpr<out USort> = constResolver.visitIlEnumConst(const)
    override fun visitIlArrayConst(const: IlArrayConst): UExpr<out USort> = constResolver.visitIlArrayConst(const)
    override fun visitIlTypeRefConst(const: IlTypeRef): UExpr<out USort> = constResolver.visitIlTypeRefConst(const)

    override fun visitErrVar(expr: IlErrVar): UExpr<out USort> {
        val key = localVarToLValue(expr)
        return scope.calcOnState { memory.read(key) }
    }

    override fun visitIlArg(expr: IlArgument): UExpr<out USort>  {
        val key = localVarToLValue(expr)
        return scope.calcOnState { memory.read(key) }
    }

    override fun visitIlLocalVar(expr: IlLocalVar): UExpr<out USort> {
        val key = localVarToLValue(expr)
        return scope.calcOnState { memory.read(key) }
    }

    fun resolveULValue(expr: IlExpr) : ULValue<*, *> {
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

    private fun arrayAccessToLValue(expr: IlArrayAccess): UArrayIndexLValue<*, *, *> = with(ctx) {
        val arrayRef = expr.array.accept(this@IlExprResolver).asExpr(addressSort)
        checkNullPointer(arrayRef)

        val index = expr.index.accept(this@IlExprResolver).asExpr(sizeSort)

        val arrayType = ctx.mockType
        val len = UArrayLengthLValue(arrayRef, arrayType, sizeSort).let {
            scope.calcOnState { memory.read(it) }
        }
        checkArrayIndexBounds(index, len)
        val boundConstr = mkBvUnsignedLessExpr(len, machineOptions.maxArraySize.toBv(sizeSort))
        scope.assert(boundConstr)

        val lvalue = UArrayIndexLValue(typeToSort(mockType), arrayRef, index, arrayType)
        lvalue
    }

    private fun fieldAccessToLValue(expr: IlFieldAccess): UFieldLValue<*, *> {
        val fieldIsStatic = expr.receiver == null
        val field = expr.field
        if (!fieldIsStatic) {
            val instance = expr.receiver!!.accept(this).asExpr(ctx.addressSort)
            checkNullPointer(instance)
            val flv = UFieldLValue(ctx.typeToSort(field.fieldType), instance, field)
            return flv
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


    override fun visitIlArrayAccess(expr: IlArrayAccess): UExpr<out USort> = scope.calcOnState {
        val key = arrayAccessToLValue(expr)
        memory.read(key)
    }

    override fun visitIlFieldAccess(expr: IlFieldAccess): UExpr<out USort> = scope.calcOnState {
        val key = fieldAccessToLValue(expr)
        memory.read(key)
    }

    override fun visitIlArrayLength(expr: IlArrayLengthExpr): UExpr<out USort> {
        val arrayRef = expr.array.accept(this).asExpr(ctx.addressSort)
        checkNullPointer(arrayRef)
        val key = UArrayLengthLValue(arrayRef, ctx.mockType, ctx.sizeSort)
        return scope.calcOnState { memory.read(key) }
    }

    override fun visitIlBinaryOp(expr: IlBinaryOp): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlBoxExpr(expr: IlBoxExpr): UExpr<out USort> {
        val resolved = expr.operand.accept(this)

        TODO("Not yet implemented")
    }

    override fun visitIlCall(expr: IlCall): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlCastClassExpr(expr: IlCastClassExpr): UExpr<out USort> = scope.calcOnState {
        val e = expr.operand.accept(this@IlExprResolver).asExpr(ctx.addressSort)
        val currType = ctx.mockType
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

    override fun visitIlConvExpr(expr: IlConvExpr): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlFieldRefConst(const: IlFieldRef): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlInitExpr(expr: IlInitExpr): UExpr<out USort> = TODO()

    override fun visitIlIsInstExpr(expr: IlIsInstExpr): UExpr<out USort> = scope.calcOnState {
        val instance = expr.operand.accept(this@IlExprResolver).asExpr(ctx.addressSort)
        memory.types.evalIsSubtype(instance, expr.expectedType)
    }

    override fun visitIlManagedDerefExpr(expr: IlManagedDerefExpr): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlManagedRefExpr(expr: IlManagedRefExpr): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlMethodRefConst(const: IlMethodRef): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlNewArrayExpr(expr: IlNewArrayExpr): UExpr<out USort> = scope.calcOnState {
        val arrayType = expr.elementType
        val size = expr.size.accept(this@IlExprResolver).asExpr(ctx.sizeSort)
        memory.allocateArray(arrayType, ctx.sizeSort, size)
    }

    override fun visitIlNewExpr(expr: IlNewExpr): UExpr<out USort> = scope.calcOnState {
        memory.allocConcrete(expr.type)
    }

    override fun visitIlSizeOfExpr(expr: IlSizeOfExpr): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlStackAllocExpr(expr: IlStackAllocExpr): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlTempVar(expr: IlTempVar): UExpr<out USort> {
        TODO("will be removed")
    }

    override fun visitIlUnaryOp(expr: IlUnaryOp): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlUnboxExpr(expr: IlUnboxExpr): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlUnmanagedDerefExpr(expr: IlUnmanagedDerefExpr): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlUnmanagedRefExpr(expr: IlUnmanagedRefExpr): UExpr<out USort> {
        TODO("Not yet implemented")
    }
}
