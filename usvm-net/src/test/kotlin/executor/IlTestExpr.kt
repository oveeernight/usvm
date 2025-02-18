package executor

import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType

sealed interface IlTestStmt

sealed interface IlTestExpr: IlTestStmt {
    val type: IlType?
}

interface IlTestConst<T>: IlTestExpr {
    val value: T

    class BoolConst(override val value: Boolean, override val type: IlType): IlTestConst<Boolean>
    class CharConst(override val value: Char, override val type: IlType): IlTestConst<Char>
    class Int8Const(override val value: Byte, override val type: IlType): IlTestConst<Byte>
    class Int16Const(override val value: Short, override val type: IlType): IlTestConst<Short>
    class Int32Const(override val value: Int, override val type: IlType): IlTestConst<Int>
    class Int64Const(override val value: Long, override val type: IlType): IlTestConst<Long>
    class UInt8Const(override val value: UByte, override val type: IlType): IlTestConst<UByte>
    class UInt16Const(override val value: UShort, override val type: IlType): IlTestConst<UShort>
    class UInt32Const(override val value: UInt, override val type: IlType): IlTestConst<UInt>
    class UInt64Const(override val value: ULong, override val type: IlType): IlTestConst<ULong>
    class FloatConst(override val value: Float, override val type: IlType): IlTestConst<Float>
    class DoubleConst(override val value: Double, override val type: IlType): IlTestConst<Double>
    class StringConst(override val value: String, override val type: IlType): IlTestConst<String>
    class NullConst(override val type: IlType): IlTestConst<Any?> {
        override val value: Any? = null
    }
}

class IlArray(val elementType: IlType, val size: Int): IlTestExpr {
    override val type: IlType? = elementType.publication.findIlTypeOrNull("${elementType.name}[]")
}

class IlObject(override val type: IlType, val fields: Map<IlField, IlTestExpr>): IlTestExpr

interface IlTestCall: IlTestExpr {
    val method: IlMethod
    val args: List<IlTestExpr>

    class InstanceMethodCall(val instance: IlTestExpr, override val method: IlMethod, override val args: List<IlTestExpr>): IlTestCall {
        override val type: IlType? = method.declaringType.publication.findIlTypeOrNull(method.returnType.name)
    }
    class StaticMethodCall(override val method: IlMethod, override val args: List<IlTestExpr>): IlTestCall {
        override val type: IlType? = method.declaringType.publication.findIlTypeOrNull(method.returnType.name)
    }
    class ConstructorCall(override val method: IlMethod, override val args: List<IlTestExpr>): IlTestCall {
        override val type: IlType = method.declaringType
    }

    class NewInstance(override val type: IlType): IlTestCall {
        override val args = emptyList<IlTestExpr>()
        override val method = error("Should not be called")
    }
}

sealed interface ArrangeStmt: IlTestStmt {
    val instance: IlTestExpr

    class SetArrayIndex(override val instance: IlTestExpr, val index: Int, val value: IlTestExpr): ArrangeStmt
    class SetObjectField(override val instance: IlTestExpr, val field: IlField, val value: IlTestExpr): ArrangeStmt
}

class IlTypeExpr(override val type: IlType): IlTestExpr
