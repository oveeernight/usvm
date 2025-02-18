package executor

import com.google.protobuf.Message
import testrunner.expressions.*
import common.DecoderApi
import org.jacodb.api.net.ilinstances.*
import org.usvm.machine.IlContext
import org.usvm.machine.logger

class IlTestExecutorDecoderApi(private val ctx: IlContext): DecoderApi<Message> {
    private val arrangeStmts = mutableListOf<Message>()

    fun arrangeStmts(): List<Message> = arrangeStmts

    override fun createBoolConst(value: Boolean): Message =
        boolConst { this.value = value; typeRepr = ctx.boolType.toTypeRepr() }
    override fun createCharConst(value: Char): Message =
        charConst { this.value = value.code; typeRepr = ctx.charType.toTypeRepr() }
    override fun createInt8Const(value: Byte): Message =
        int8Const { this.value = value.toInt(); typeRepr = ctx.charType.toTypeRepr() }
    override fun createInt16Const(value: Short): Message =
        int16Const { this.value = value.toInt(); typeRepr = ctx.charType.toTypeRepr() }
    override fun createInt32Const(value: Int): Message =
        int32Const { this.value = value; typeRepr = ctx.charType.toTypeRepr() }
    override fun createInt64Const(value: Long): Message =
        int64Const { this.value = value; typeRepr = ctx.charType.toTypeRepr() }
    override fun createUInt8Const(value: UByte): Message =
        uInt8Const { this.value = value.toInt(); typeRepr = ctx.uint8Type.toTypeRepr() }
    override fun createUInt16Const(value: UShort): Message =
        uInt16Const { this.value = value.toInt(); typeRepr = ctx.uint16Type.toTypeRepr() }
    override fun createUInt32Const(value: UInt): Message =
        uInt32Const { this.value = value.toInt(); typeRepr = ctx.uint32Type.toTypeRepr() }
    override fun createUInt64Const(value: ULong): Message =
        uInt64Const { this.value = value.toLong(); typeRepr = ctx.uint64Type.toTypeRepr() }
    override fun createFloatConst(value: Float): Message =
        floatConst { this.value = value; typeRepr = ctx.floatType.toTypeRepr() }
    override fun createDoubleConst(value: Double): Message =
        doubleConst { this.value = value; typeRepr = ctx.doubleType.toTypeRepr() }
    override fun createStringConst(value: String): Message =
       stringConst { this.value = value; typeRepr = ctx.stringType.toTypeRepr() }
    override fun createNullConst(type: IlType): Message =
        nullConst { typeRepr = type.toTypeRepr() }
    override fun createArray(elementType: IlType, size: Int, address: Int): Message =
        arrayInstance { elementTypeRepr = elementType.toTypeRepr(); this.size = size; this.address = address }
    override fun createObject(type: IlType, address: Int): Message {
        return objectInstance { typeRepr = type.toTypeRepr(); this.address = address }
    }
    override fun createCyclicReference(type: IlType, address: Int): Message =
        cyclicReference { typeRepr = type.toTypeRepr(); this.address = address }
    // TODO ctor call
    override fun callMethod(method: IlMethod, args: List<Message>): Message {
        val argsPacked = args.map { it.pack() }
        val returnType = method.returnType.toTypeRepr()
        val methodRepr = method.toMethodRepr()
        return methodCall {
            returnTypeRepr = returnType
            this.methodRepr = methodRepr
            this.args.addAll(argsPacked)
        }
    }

    override fun setObjectField(obj: Message, field: IlField, value: Message) {
        obj as TestExpressions.ObjectInstance
        val valueAsAny = value.pack()
        val set = setObjectField {
            this.instance = obj
            this.fieldRepr = field.toFieldRepr()
            this.value = valueAsAny
        }

        arrangeStmts += set
    }

    override fun setArrayIndex(array: Message, index: Int, value: Message) {
        array as TestExpressions.ArrayInstance
        val valueAsAny = value.pack()
        val set = setArrayIndex {
            this.instance = array
            this.index = index
            this.value = valueAsAny
        }
        logger.error { "set: $set" }
        arrangeStmts += set
    }
}

private fun IlType.toTypeRepr(): TestExpressions.TypeRepr {
    val mdlToken = moduleToken
    val tpToken = typeToken
    val type = typeRepr {
        asm = asmName
        moduleToken = mdlToken
        typeToken = tpToken
    }
    type.genericArgsList.addAll(genericArgs.map { it.toTypeRepr() })

    return type
}


private fun IlMethod.toMethodRepr(): TestExpressions.MethodRepr {
    val declType = declaringType
    val signature = signature
    val name = name
    return methodRepr {
        this.declType = declType.toTypeRepr()
        this.signature = signature
        this.name = name
    }
}

private fun IlField.toFieldRepr(): TestExpressions.FieldRepr {
    val name = name
    val typeRepr = fieldType.toTypeRepr()
    return fieldRepr {
        this.typeRepr = typeRepr
        this.name = name
    }
}


fun Message.pack() = com.google.protobuf.Any.pack(this)
