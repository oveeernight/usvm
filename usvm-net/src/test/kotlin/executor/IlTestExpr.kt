package executor

import kotlinx.serialization.Serializable

@Serializable
sealed interface IlTestStmt {
    val kind: StmtKind
}

@Serializable
data class TypeRepr(val asm: String, val moduleToken: Int, val typeToken: Int, val genericArgs: List<TypeRepr>)

@Serializable
data class MethodRepr(val declType: TypeRepr, val signature: String, val name: String)

@Serializable
data class FieldRepr(val typeRepr: TypeRepr, val name: String)

@Serializable
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
    val typeRepr: TypeRepr?
}

@Serializable
sealed interface IlTestConst<T> : IlTestExpr {
    val value: T

    @Serializable
    class BoolConst(override val kind: StmtKind, override val value: Boolean, override val typeRepr: TypeRepr) :
        IlTestConst<Boolean>

    @Serializable
    class CharConst(override val kind: StmtKind, override val value: Char, override val typeRepr: TypeRepr) :
        IlTestConst<Char>

    @Serializable
    class Int8Const(override val kind: StmtKind, override val value: Byte, override val typeRepr: TypeRepr) : IlTestConst<Byte>
        @Serializable
    class Int16Const(override val kind: StmtKind, override val value: Short, override val typeRepr: TypeRepr) :
        IlTestConst<Short>

    @Serializable
    class Int32Const(override val kind: StmtKind, override val value: Int, override val typeRepr: TypeRepr) :
        IlTestConst<Int>

    @Serializable
    class Int64Const(override val kind: StmtKind, override val value: Long, override val typeRepr: TypeRepr) :
        IlTestConst<Long>

    @Serializable
    class UInt8Const(override val kind: StmtKind, override val value: UByte, override val typeRepr: TypeRepr) :
        IlTestConst<UByte>

    @Serializable
    class UInt16Const(override val kind: StmtKind, override val value: UShort, override val typeRepr: TypeRepr) :
        IlTestConst<UShort>

    @Serializable
    class UInt32Const(override val kind: StmtKind, override val value: UInt, override val typeRepr: TypeRepr) :
        IlTestConst<UInt>

    @Serializable
    class UInt64Const(override val kind: StmtKind, override val value: ULong, override val typeRepr: TypeRepr) :
        IlTestConst<ULong>

    @Serializable
    class FloatConst(override val kind: StmtKind, override val value: Float, override val typeRepr: TypeRepr) :
        IlTestConst<Float>

    @Serializable
    class DoubleConst(override val kind: StmtKind, override val value: Double, override val typeRepr: TypeRepr) :
        IlTestConst<Double>

    @Serializable
    class StringConst(override val kind: StmtKind, override val value: String, override val typeRepr: TypeRepr) :
        IlTestConst<String>
}

@Serializable
class NullConst(override val kind: StmtKind, override val typeRepr: TypeRepr): IlTestExpr

@Serializable
class ArrayInstance(override val kind: StmtKind, override val typeRepr: TypeRepr, val size: Int, val address: Int) :
    IlTestExpr

@Serializable
class ObjectInstance(override val kind: StmtKind, override val typeRepr: TypeRepr, val address: Int) : IlTestExpr

// TODO method generic args
@Serializable
sealed interface IlTestCall : IlTestExpr {
    val method: MethodRepr
    val args: List<IlTestExpr>

    @Serializable
    class InstanceMethodCall(
        override val kind: StmtKind,
        override val method: MethodRepr,
        override val typeRepr: TypeRepr,
        val instance: IlTestExpr,
        override val args: List<IlTestExpr>
    ) : IlTestCall

    @Serializable
    class StaticMethodCall(
        override val kind: StmtKind,
        override val method: MethodRepr,
        override val typeRepr: TypeRepr,
        override val args: List<IlTestExpr>
    ) : IlTestCall

    @Serializable
    class ConstructorCall(
        override val kind: StmtKind,
        override val method: MethodRepr,
        override val typeRepr: TypeRepr,
        override val args: List<IlTestExpr>
    ) : IlTestCall
}

@Serializable
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
        val field: FieldRepr,
        val value: IlTestExpr
    ) : ArrangeStmt
}

@Serializable
class IlTypeInstance(override val kind: StmtKind, override val typeRepr: TypeRepr) : IlTestExpr

@Serializable
class CyclicReference(override val kind: StmtKind, override val typeRepr: TypeRepr, val address: Int) : IlTestExpr
