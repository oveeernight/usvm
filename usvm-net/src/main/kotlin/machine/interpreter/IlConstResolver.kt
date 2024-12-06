package org.usvm.machine.interpreter

import org.example.ilinstances.IlType
import org.jacodb.api.net.core.IlConstVisitor
import org.jacodb.api.net.ilinstances.*
import org.usvm.UConcreteHeapRef
import org.usvm.UExpr
import org.usvm.UHeapRef
import org.usvm.USort
import org.usvm.api.allocateArrayInitialized
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.IlContext

class IlConstResolver(
    val ctx: IlContext,
    val scope: IlStepScope,
    private val getOrMkStringConst: (String) -> UConcreteHeapRef,
    private val getOrMkTypeRef: (IlType) -> UConcreteHeapRef,
): IlConstVisitor<UExpr<out USort>> {
    override fun visitIlArrayConst(const: IlArrayConst): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlBoolConst(const: IlBoolConst): UExpr<out USort> = with(ctx) {
        mkBool(const.value)
    }

    override fun visitIlCharConst(const: IlCharConst): UExpr<out USort> = with(ctx) {
        mkBv(const.value.code, charSort)
    }

    override fun visitIlFloatConst(const: IlFloatConst): UExpr<out USort> = with(ctx) {
        mkFp(const.value, floatSort)
    }

    override fun visitIlDoubleConst(const: IlDoubleConst): UExpr<out USort> = with(ctx) {
        mkFp(const.value, doubleSort)
    }

    override fun visitIlEnumConst(const: IlEnumConst): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlFieldRefConst(const: IlFieldRef): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlInt8Const(const: IlInt8Const): UExpr<out USort> = with(ctx) {
        mkBv(const.value, byteSort)
    }


    override fun visitIlInt16Const(const: IlInt16Const): UExpr<out USort> = with(ctx) {
        mkBv(const.value, byteSort)
    }

    override fun visitIlInt32Const(const: IlInt32Const): UExpr<out USort> = with(ctx) {
        mkBv(const.value, byteSort)
    }

    override fun visitIlInt64Const(const: IlInt64Const): UExpr<out USort> = with(ctx) {
        mkBv(const.value, byteSort)
    }

    override fun visitIlUInt8Const(const: IlUInt8Const): UExpr<out USort> = with(ctx) {
         TODO()
//        mkBvUnsigned(const.value, bytesBitSize)
    }

    override fun visitIlUInt16Const(const: IlUInt16Const): UExpr<out USort> = with(ctx) {
        TODO()
//        mkBv (const.value, byteSort)
    }

    override fun visitIlUInt32Const(const: IlUInt32Const): UExpr<out USort> = with(ctx) {
        TODO()
//            mkBv(const.value, byteSort)
    }

    override fun visitIlUInt64Const(const: IlUInt64Const): UExpr<out USort> = with(ctx) {
        TODO()
//            mkBv(const.value, byteSort)
    }

    override fun visitIlMethodRefConst(const: IlMethodRef): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlNullConst(const: IlNull): UExpr<out USort>  = with(ctx) {
        nullRef
    }
    override fun visitIlStringConst(const: IlStringConst): UExpr<out USort> = scope.calcOnState {
        val strRef = getOrMkStringConst(const.value)

        val values = const.value.asSequence().map { ctx.mkBv(it.code, ctx.charSort) }
        val arrayDesc = ctx.charType

        val arrRef = memory.allocateArrayInitialized(arrayDesc, ctx.charSort,  ctx.bv32Sort, values)

        arrRef
        TODO("think about it")
    }
    override fun visitIlTypeRefConst(const: IlTypeRef): UExpr<out USort> = resolveTypeRef(const.referencedType)

    internal fun resolveTypeRef(type: IlType) : UConcreteHeapRef = scope.calcOnState {
        with(ctx) {
            val ref = getOrMkTypeRef(type)

            // redirect typeof(const) to [type]
            val fieldTypeLValue = UFieldLValue(addressSort, ref, syntheticTypeField)
            val selfAddress = memory.allocStatic(type)
            memory.write(fieldTypeLValue, selfAddress, guard = trueExpr)

            // type is also System.Type
            memory.types.allocate(ref.address, systemType)

            ref
        }
    }
}
