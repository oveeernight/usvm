package org.usvm.machine.interpreter

import org.jacodb.api.net.core.IlConstVisitor
import org.jacodb.api.net.ilinstances.*
import org.usvm.UConcreteHeapRef
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.api.allocateArrayInitialized
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.IlContext

class IlConstResolver(
    val ctx: IlContext,
    val scope: IlStepScope,
    private val strings: MutableMap<String, UConcreteHeapRef>,
    private val getOrMkTypeRef: (IlType) -> UConcreteHeapRef,
): IlConstVisitor<UExpr<out USort>> {
    override fun visitIlArrayConst(const: IlArrayConstant): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlBoolConst(const: IlBoolConstant): UExpr<out USort> = with(ctx) {
        mkBool(const.value)
    }

    override fun visitIlCharConst(const: IlCharConstant): UExpr<out USort> = with(ctx) {
        mkBv(const.value.code, charSort)
    }

    override fun visitIlFloatConst(const: IlFloatConstant): UExpr<out USort> = with(ctx) {
        mkFp(const.value, floatSort)
    }

    override fun visitIlDoubleConst(const: IlDoubleConstant): UExpr<out USort> = with(ctx) {
        mkFp(const.value, doubleSort)
    }

    override fun visitIlEnumConst(const: IlEnumConstant): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlFieldRefConst(const: IlFieldRef): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlInt8Const(const: IlInt8Constant): UExpr<out USort> = with(ctx) {
        mkBv(const.value, int8sort)
    }


    override fun visitIlInt16Const(const: IlInt16Constant): UExpr<out USort> = with(ctx) {
        mkBv(const.value, int16sort)
    }

    override fun visitIlInt32Const(const: IlInt32Constant): UExpr<out USort> = with(ctx) {
        mkBv(const.value, int32sort)
    }

    override fun visitIlInt64Const(const: IlInt64Constant): UExpr<out USort> = with(ctx) {
        mkBv(const.value, int64sort)
    }

    override fun visitIlUInt8Const(const: IlUInt8Constant): UExpr<out USort> = with(ctx) {
         TODO()
//        mkBvUnsigned(const.value, bytesBitSize)
    }

    override fun visitIlUInt16Const(const: IlUInt16Constant): UExpr<out USort> = with(ctx) {
        TODO()
//        mkBv (const.value, byteSort)
    }

    override fun visitIlUInt32Const(const: IlUInt32Constant): UExpr<out USort> = with(ctx) {
        TODO()
//            mkBv(const.value, byteSort)
    }

    override fun visitIlUInt64Const(const: IlUInt64Constant): UExpr<out USort> = with(ctx) {
        TODO()
//            mkBv(const.value, byteSort)
    }

    override fun visitIlMethodRefConst(const: IlMethodRef): UExpr<out USort> {
        TODO("Not yet implemented")
    }

    override fun visitIlNullConst(const: IlNull): UExpr<out USort>  = with(ctx) {
        nullRef
    }
    override fun visitIlStringConst(const: IlStringConstant): UExpr<out USort> {
        val strRef = strings[const.value]
        if (strRef != null) return strRef

        val values = const.value.asSequence().map { ctx.mkBv(it.code, ctx.charSort) }
        val arrayDesc = ctx.charType

        val charArrayRef = scope.calcOnState {
            memory.allocateArrayInitialized(arrayDesc, ctx.charSort,  ctx.bv32Sort, values)
        }

        strings[const.value] = charArrayRef

        return charArrayRef
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
