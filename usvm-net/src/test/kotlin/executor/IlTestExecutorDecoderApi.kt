package executor

import common.DecoderApi
import org.jacodb.api.net.ilinstances.*
import org.usvm.machine.IlContext

class IlTestExecutorDecoderApi(val ctx: IlContext): DecoderApi<IlTestExpr> {
    private val publication = ctx.publication
    private val arrangeStmts = mutableListOf<IlTestStmt>()

    fun arrangeStmts(): List<IlTestStmt> = arrangeStmts

    override fun createBoolConst(value: Boolean): IlTestExpr = IlTestConst.BoolConst(value, ctx.boolType)
    override fun createCharConst(value: Char): IlTestExpr = IlTestConst.CharConst(value, ctx.charType)
    override fun createInt8Const(value: Byte): IlTestExpr = IlTestConst.Int8Const(value, ctx.int8Type)
    override fun createInt16Const(value: Short): IlTestExpr = IlTestConst.Int16Const(value, ctx.int16Type)
    override fun createInt32Const(value: Int): IlTestExpr = IlTestConst.Int32Const(value, ctx.int32Type)
    override fun createInt64Const(value: Long): IlTestExpr = IlTestConst.Int64Const(value, ctx.int64Type)
    override fun createUInt8Const(value: UByte): IlTestExpr = IlTestConst.UInt8Const(value, ctx.uint8Type)
    override fun createUInt16Const(value: UShort): IlTestExpr = IlTestConst.UInt16Const(value, ctx.uint16Type)
    override fun createUInt32Const(value: UInt): IlTestExpr = IlTestConst.UInt32Const(value, ctx.uint32Type)
    override fun createUInt64Const(value: ULong): IlTestExpr = IlTestConst.UInt64Const(value, ctx.uint64Type)
    override fun createFloatConst(value: Float): IlTestExpr = IlTestConst.FloatConst(value, ctx.floatType)
    override fun createDoubleConst(value: Double): IlTestExpr = IlTestConst.DoubleConst(value, ctx.doubleType)
    override fun createStringConst(value: String): IlTestExpr = IlTestConst.StringConst(value, ctx.doubleType)
    override fun createNullConst(type: IlType): IlTestExpr = IlTestConst.NullConst(type)
    override fun createArray(elementType: IlType, size: Int): IlTestExpr = IlArray(elementType, size)
    override fun createObject(type: IlType): IlTestExpr = IlTestCall.NewInstance(type)

    override fun callMethod(method: IlMethod, args: List<IlTestExpr>): IlTestExpr {
        return if (method.isStatic) IlTestCall.StaticMethodCall(method, args)
        else IlTestCall.InstanceMethodCall(args[0], method, args.drop(1))
    }

    override fun setObjectField(obj: IlTestExpr, field: IlField, value: IlTestExpr) {
        arrangeStmts += ArrangeStmt.SetObjectField(obj, field, value)
    }

    override fun setArrayIndex(array: IlTestExpr, index: Int, value: IlTestExpr) {
        arrangeStmts += ArrangeStmt.SetArrayIndex(array, index, value)
    }
}
