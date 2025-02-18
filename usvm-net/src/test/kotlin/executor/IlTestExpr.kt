package executor

import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType

sealed interface IlTestStmt

data class TypeRepr(val asm: String, val moduleToken: Int, val typeToken: Int, val genericArgs: List<TypeRepr>)
data class MethodRepr(val signature: String, val name: String)

sealed interface IlTestExpr: IlTestStmt {
    val type: TypeRepr?
}


sealed interface IlTestConst<T>: IlTestExpr {
    val value: T

    class BoolConst(override val value: Boolean, type: IlType): IlTestConst<Boolean> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class CharConst(override val value: Char,  type: IlType): IlTestConst<Char> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class Int8Const(override val value: Byte, type: IlType): IlTestConst<Byte> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class Int16Const(override val value: Short, type: IlType): IlTestConst<Short> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class Int32Const(override val value: Int, type: IlType): IlTestConst<Int> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class Int64Const(override val value: Long, type: IlType): IlTestConst<Long> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class UInt8Const(override val value: UByte, type: IlType): IlTestConst<UByte> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class UInt16Const(override val value: UShort, type: IlType): IlTestConst<UShort> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class UInt32Const(override val value: UInt, type: IlType): IlTestConst<UInt> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class UInt64Const(override val value: ULong, type: IlType): IlTestConst<ULong> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class FloatConst(override val value: Float, type: IlType): IlTestConst<Float>{
        override val type: TypeRepr = type.toTypeRepr()
    }
    class DoubleConst(override val value: Double, type: IlType): IlTestConst<Double> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class StringConst(override val value: String, type: IlType): IlTestConst<String> {
        override val type: TypeRepr = type.toTypeRepr()
    }
    class NullConst(type: IlType): IlTestConst<Any?> {
        override val value: Any? = null
        override val type: TypeRepr = type.toTypeRepr()
    }
}

class ArrayInstance(type: IlType, val size: Int, val address: Int): IlTestExpr {
    override val type: TypeRepr = type.toTypeRepr()
}

class IlObject(type: IlType): IlTestExpr {
    override val type: TypeRepr = type.toTypeRepr()
}

class ObjectInstance(type: IlType, val address: Int): IlTestExpr {
    override val type: TypeRepr = type.toTypeRepr()
}

interface IlTestCall: IlTestExpr {
    val method: MethodRepr
    val args: List<IlTestExpr>

    class InstanceMethodCall(val instance: IlTestExpr, method: IlMethod, override val args: List<IlTestExpr>): IlTestCall {
        override val type: TypeRepr? = method.declaringType.publication.findIlTypeOrNull(method.returnType.name)?.toTypeRepr()
        override val method: MethodRepr = method.toMethodRepr()
    }
    class StaticMethodCall(method: IlMethod, override val args: List<IlTestExpr>): IlTestCall {
        override val type: TypeRepr? = method.declaringType.publication.findIlTypeOrNull(method.returnType.name)?.toTypeRepr()
        override val method: MethodRepr = method.toMethodRepr()
    }
    class ConstructorCall(method: IlMethod, override val args: List<IlTestExpr>): IlTestCall {
        override val type: TypeRepr = method.declaringType.toTypeRepr()
        override val method: MethodRepr = method.toMethodRepr()
    }
}

sealed interface ArrangeStmt: IlTestStmt {
    val instance: IlTestExpr

    class SetArrayIndex(override val instance: IlTestExpr, val index: Int, val value: IlTestExpr): ArrangeStmt
    class SetObjectField(override val instance: IlTestExpr, val field: IlField, val value: IlTestExpr): ArrangeStmt
}

class IlTypeExpr(type: IlType): IlTestExpr {
    override val type: TypeRepr = type.toTypeRepr()
}

private fun IlType.toTypeRepr(): TypeRepr =
    TypeRepr(asmName, moduleToken, typeToken, genericArgs.map { it.toTypeRepr()})
private fun IlMethod.toMethodRepr(): MethodRepr = MethodRepr(signature, name)
