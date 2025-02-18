package executor

import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
import kotlinx.serialization.Serializable

@Serializable
sealed interface IlTestStmt {
    val kind: StmtKind
}

@Serializable
data class TypeRepr(val asm: String, val moduleToken: Int, val typeToken: Int, val genericArgs: List<TypeRepr>)

@Serializable
data class MethodRepr(val declType: TypeRepr, val signature: String, val name: String)

enum class StmtKind {
    BOOL, CHAR, INT8, INT16, INT32, INT64, UINT8, UINT16, UINT32, UINT64, FLOAT, DOUBLE,
    STRING, NULL,
    INSTANCE_CALL, STATIC_CALL, CONSTRUCTOR_CALL,
    NEW_OBJ, NEW_ARRAY,
    SET_OBJ_FIELD, SET_ARRAY_INDEX,
    TYPE_INSTANCE,
    CYCLIC_REFERENCE
}

@Serializable
sealed interface IlTestExpr : IlTestStmt {
    val type: TypeRepr?
}

sealed interface IlTestConst<T> : IlTestExpr {
    val value: T

    @Serializable
    class BoolConst(override val kind: StmtKind, override val value: Boolean, override val type: TypeRepr) :
        IlTestConst<Boolean>

    @Serializable
    class CharConst(override val kind: StmtKind, override val value: Char, override val type: TypeRepr) :
        IlTestConst<Char>

    @Serializable
    class Int8Const(override val kind: StmtKind, override val value: Byte, override val type: TypeRepr) : IlTestConst<Byte>
        @Serializable
    class Int16Const(override val kind: StmtKind, override val value: Short, override val type: TypeRepr) :
        IlTestConst<Short>

    @Serializable
    class Int32Const(override val kind: StmtKind, override val value: Int, override val type: TypeRepr) :
        IlTestConst<Int>

    @Serializable
    class Int64Const(override val kind: StmtKind, override val value: Long, override val type: TypeRepr) :
        IlTestConst<Long>

    @Serializable
    class UInt8Const(override val kind: StmtKind, override val value: UByte, override val type: TypeRepr) :
        IlTestConst<UByte>

    @Serializable
    class UInt16Const(override val kind: StmtKind, override val value: UShort, override val type: TypeRepr) :
        IlTestConst<UShort>

    @Serializable
    class UInt32Const(override val kind: StmtKind, override val value: UInt, override val type: TypeRepr) :
        IlTestConst<UInt>

    class UInt64Const(override val kind: StmtKind, override val value: ULong, override val type: TypeRepr) :
        IlTestConst<ULong>

    class FloatConst(override val kind: StmtKind, override val value: Float, override val type: TypeRepr) :
        IlTestConst<Float>

    @Serializable
    class DoubleConst(override val kind: StmtKind, override val value: Double, override val type: TypeRepr) :
        IlTestConst<Double>

    @Serializable
    class StringConst(override val kind: StmtKind, override val value: String, override val type: TypeRepr) :
        IlTestConst<String>

    @Serializable
    class NullConst(override val kind: StmtKind, override val type: TypeRepr, override val value: Any? = null) :
        IlTestConst<Any?>
}

@Serializable
class ArrayInstance(override val kind: StmtKind, override val type: TypeRepr, val size: Int, val address: Int) :
    IlTestExpr

@Serializable
class ObjectInstance(override val kind: StmtKind, override val type: TypeRepr, val address: Int) : IlTestExpr

@Serializable
sealed interface IlTestCall : IlTestExpr {
    val method: MethodRepr
    val args: List<IlTestExpr>

    @Serializable
    class InstanceMethodCall(
        override val kind: StmtKind,
        override val method: MethodRepr,
        override val type: TypeRepr,
        val instance: IlTestExpr,
        override val args: List<IlTestExpr>
    ) : IlTestCall

    @Serializable
    class StaticMethodCall(
        override val kind: StmtKind,
        override val method: MethodRepr,
        override val type: TypeRepr,
        override val args: List<IlTestExpr>
    ) : IlTestCall

    @Serializable
    class ConstructorCall(
        override val kind: StmtKind,
        override val method: MethodRepr,
        override val type: TypeRepr,
        override val args: List<IlTestExpr>
    ) : IlTestCall
}

sealed interface ArrangeStmt : IlTestStmt {
    val instance: IlTestExpr

    @Serializable
    class SetArrayIndex(
        override val kind: StmtKind,
        override val instance: IlTestExpr,
        val index: Int,
        val value: IlTestExpr
    ) : ArrangeStmt

    @Serializable
    class SetObjectField(
        override val kind: StmtKind,
        override val instance: IlTestExpr,
        val field: IlField,
        val value: IlTestExpr
    ) : ArrangeStmt
}

@Serializable
class IlTypeInstance(override val kind: StmtKind, override val type: TypeRepr) : IlTestExpr

@Serializable
class CyclicReference(override val kind: StmtKind, override val type: TypeRepr, val address: Int) : IlTestExpr
