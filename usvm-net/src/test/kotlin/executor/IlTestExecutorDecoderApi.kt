package executor

import common.DecoderApi
import org.jacodb.api.net.ilinstances.*
import org.usvm.machine.IlContext

class IlTestExecutorDecoderApi(val ctx: IlContext): DecoderApi<IlTestExpr> {
    private val arrangeStmts = mutableListOf<IlTestStmt>()

    fun arrangeStmts(): List<IlTestStmt> = arrangeStmts

    override fun createBoolConst(value: Boolean): IlTestExpr =
        IlTestConst.BoolConst(StmtKind.BOOL, value, ctx.boolType.toTypeRepr())
    override fun createCharConst(value: Char): IlTestExpr =
        IlTestConst.CharConst(StmtKind.CHAR, value, ctx.charType.toTypeRepr())
    override fun createInt8Const(value: Byte): IlTestExpr =
        IlTestConst.Int8Const(StmtKind.INT8, value, ctx.int8Type.toTypeRepr())
    override fun createInt16Const(value: Short): IlTestExpr =
        IlTestConst.Int16Const(StmtKind.INT16, value, ctx.int16Type.toTypeRepr())
    override fun createInt32Const(value: Int): IlTestExpr =
        IlTestConst.Int32Const(StmtKind.INT32, value, ctx.int32Type.toTypeRepr())
    override fun createInt64Const(value: Long): IlTestExpr =
        IlTestConst.Int64Const(StmtKind.INT64, value, ctx.int64Type.toTypeRepr())
    override fun createUInt8Const(value: UByte): IlTestExpr =
        IlTestConst.UInt8Const(StmtKind.UINT8, value, ctx.uint8Type.toTypeRepr())
    override fun createUInt16Const(value: UShort): IlTestExpr =
        IlTestConst.UInt16Const(StmtKind.UINT16, value, ctx.uint16Type.toTypeRepr())
    override fun createUInt32Const(value: UInt): IlTestExpr =
        IlTestConst.UInt32Const(StmtKind.UINT32, value, ctx.uint32Type.toTypeRepr())
    override fun createUInt64Const(value: ULong): IlTestExpr =
        IlTestConst.UInt64Const(StmtKind.UINT64, value, ctx.uint64Type.toTypeRepr())
    override fun createFloatConst(value: Float): IlTestExpr =
        IlTestConst.FloatConst(StmtKind.FLOAT, value, ctx.floatType.toTypeRepr())
    override fun createDoubleConst(value: Double): IlTestExpr =
        IlTestConst.DoubleConst(StmtKind.DOUBLE, value, ctx.doubleType.toTypeRepr())
    override fun createStringConst(value: String): IlTestExpr =
        IlTestConst.StringConst(StmtKind.STRING, value, ctx.stringType.toTypeRepr())
    override fun createNullConst(type: IlType): IlTestExpr =
        IlTestConst.NullConst(StmtKind.NULL, type.toTypeRepr())
    override fun createArray(type: IlType, size: Int, address: Int): IlTestExpr =
        ArrayInstance(StmtKind.NEW_ARRAY, type.toTypeRepr(), size, address)
    override fun createObject(type: IlType, address: Int): IlTestExpr =
        ObjectInstance(StmtKind.NEW_OBJ, type.toTypeRepr(), address)

    override fun createCyclicReference(type: IlType, address: Int): IlTestExpr =
        CyclicReference(StmtKind.CYCLIC_REFERENCE, type.toTypeRepr(), address)

    // TODO ctor call
    override fun callMethod(method: IlMethod, args: List<IlTestExpr>): IlTestExpr {
        val returnType = method.returnType.toTypeRepr()
        return if (method.isStatic) {
            IlTestCall.StaticMethodCall(StmtKind.STATIC_CALL, method.toMethodRepr(), returnType, args)
        }
        else {
            IlTestCall.InstanceMethodCall(
                StmtKind.INSTANCE_CALL,
                method.toMethodRepr(),
                returnType,
                args[0],
                args.drop(1)
            ).also { arrangeStmts.add(it) }
        }
    }

    override fun setObjectField(obj: IlTestExpr, field: IlField, value: IlTestExpr) {
        arrangeStmts += ArrangeStmt.SetObjectField(StmtKind.SET_OBJ_FIELD, obj, field, value)
    }

    override fun setArrayIndex(array: IlTestExpr, index: Int, value: IlTestExpr) {
        arrangeStmts += ArrangeStmt.SetArrayIndex(StmtKind.SET_ARRAY_INDEX, array, index, value)
    }
}

private fun IlType.toTypeRepr(): TypeRepr =
    TypeRepr(asmName, moduleToken, typeToken, genericArgs.map { it.toTypeRepr() })

private fun IlMethod.toMethodRepr(): MethodRepr = MethodRepr(declaringType.toTypeRepr(), signature, name)
