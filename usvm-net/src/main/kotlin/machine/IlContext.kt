package org.usvm.machine

import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.IlFieldDto
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.*
import org.jacodb.api.net.publication.IlPredefinedAsmExt.mscorelib
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.state.boxed.IlInputBoxedValuesId
import org.usvm.machine.state.IlStaticFieldsRegionId
import org.usvm.memory.ULValue
import org.usvm.memory.URegisterStackLValue
import org.usvm.memory.USymbolicCollection

typealias USizeSort = UBv32Sort

class IlContext(val publication: IlPublication, components: IlComponents) : UContext<USizeSort>(components) {
    //
    private val mscorelib by lazy { publication.mscorelib() }
    val boolType by lazy { findTypeOrReportAbsence("Boolean", mscorelib) }
    val charType by lazy { findTypeOrReportAbsence("Char", mscorelib) }
    val int8Type by lazy { findTypeOrReportAbsence("SByte", mscorelib) }
    val int16Type by lazy { findTypeOrReportAbsence("Int16", mscorelib) }
    val int32Type by lazy { findTypeOrReportAbsence("Int32", mscorelib) }
    val int64Type by lazy { findTypeOrReportAbsence("Int64", mscorelib) }
    val uint8Type by lazy { findTypeOrReportAbsence("Byte", mscorelib) }
    val uint16Type by lazy { findTypeOrReportAbsence("UInt16", mscorelib) }
    val uint32Type by lazy { findTypeOrReportAbsence("UInt32", mscorelib) }
    val uint64Type by lazy { findTypeOrReportAbsence("UInt64", mscorelib) }
    val floatType by lazy { findTypeOrReportAbsence("Single", mscorelib) }
    val doubleType by lazy { findTypeOrReportAbsence("Double", mscorelib) }
    val stringType by lazy { findTypeOrReportAbsence("String", mscorelib) }
    val intPtrType by lazy { findTypeOrReportAbsence("IntPtr", mscorelib) }
    val uintPtrType by lazy { findTypeOrReportAbsence("UIntPtr", mscorelib) }

    val primitiveTypes: Set<IlType> by lazy {
        buildSet {
            add(boolType)
            add(charType)
            add(int8Type)
            add(uint8Type)
            add(int16Type)
            add(uint16Type)
            add(int32Type)
            add(uint32Type)
            add(int64Type)
            add(uint64Type)
            add(floatType)
            add(doubleType)

        }
    }

    val valueType by lazy { findTypeOrReportAbsence("ValueType", mscorelib) }
    val nullableType by lazy { findTypeOrReportAbsence("Nullable", mscorelib) }
    val voidType by lazy { findTypeOrReportAbsence("Void", mscorelib) }
    val objectType by lazy { findTypeOrReportAbsence("Object", mscorelib) }
    val systemType by lazy { findTypeOrReportAbsence("Type", mscorelib) }

    val int8sort = bv8Sort
    val charSort = bv16Sort
    val int16sort = bv16Sort
    val int32sort = bv32Sort
    val int64sort = bv64Sort
    val floatSort = fp32Sort
    val doubleSort = fp64Sort
    val voidSort by lazy { VoidSort(this) }
    val sizeSort = bv32Sort

    val byteBitSize = 8u
    val shortBitSize = 16u
    val intBitSize = 32u
    val longBitSize = 64u

    val void by lazy { VoidValue(this) }

//    fun <Key, Sort: USort> mkManagedRef(key: ULValue<Key, Sort>, type: IlType) : IlManagedHeapRef<Key, Sort> =
//        IlManagedHeapRef(this, type, key)

    fun mkPtr(
        base: ULValue<*, *>?,
        baseType: IlType,
        offset: UExpr<UBvSort>,
        sightType: IlType
    ): IlPtr = IlPtr(this, base, baseType, offset, sightType)

    fun <Sort : USort> mkStaticFieldReading(
        sort: Sort,
        regionId: IlStaticFieldsRegionId<Sort>,
        field: IlField
    ): IlStaticFieldReading<Sort> =
        IlStaticFieldReading(this, sort, regionId, field)

    fun <Sort: USort> mkBoxedValueReading(
        ref: UHeapRef,
        collection: USymbolicCollection<IlInputBoxedValuesId<Sort>, UHeapRef, Sort>,
    ) = IlInputBoxedValueReading(this, ref, collection)

//    fun mkDetachedPtr(offset: UExpr<UBvSort>, sightType: IlType): IlPtr<UAddressSort> {
//        val location = IlHeapLocation(nullRef, addressSort, sightType, isArray = false)
//        return IlPtr(this, location, offset, sightType)
//    }

    val zeroField: IlField by lazy {
        val dto = IlFieldDto(
            fieldType = TypeId(typeArgs = emptyList(), asmName = mscorelib, typeName = "System.Byte"),
            isStatic = false,
            name = "__zero__",
            attrs = emptyList(),
            isConstructed = false,
            offset = 0
        )
        IlFieldImpl(declaringType = objectType, dto = dto, typeLoader = publication)
    }

    val syntheticTypeField : IlField by lazy {
        val dto = IlFieldDto(
            fieldType = TypeId(asmName = mscorelib, typeName = "Type", typeArgs = emptyList()),
            isStatic = false,
            name = "type",
            attrs = emptyList(),
            isConstructed = false, // idk,
            offset = 0
        )

        IlFieldImpl(systemType as IlTypeImpl, dto, publication)
    }

    val syntheticStructLocationField : IlField by lazy {
        val dto = IlFieldDto(
            fieldType = TypeId(asmName = mscorelib, typeName = objectType.name, typeArgs = emptyList()),
            isStatic = false,
            name = "__location__",
            attrs = emptyList(),
            isConstructed = false,
            offset = 0
        )
        IlFieldImpl(valueType, dto, publication)
    }

    val staticFieldsInitializedFlag: IlField by lazy {
        val dto = IlFieldDto(
            fieldType = TypeId(asmName = mscorelib, typeName = boolType.name, typeArgs = emptyList()),
            isStatic = true,
            name = "__staticFieldsInitialized__",
            attrs = emptyList(),
            isConstructed = false,
            offset = 0
        )
        IlFieldImpl(boolType, dto, publication)
    }

    fun typeToSort(type: IlType): USort {
        if (type is IlEnumType) return typeToSort(type.underlyingType)
        return when (type) {
            boolType -> boolSort
            charType -> charSort
            int8Type -> int8sort
            uint8Type -> int8sort
            int16Type -> int16sort
            uint16Type -> int16sort
            int32Type -> int32sort
            uint32Type -> int32sort
            int64Type -> int64sort
            uint64Type -> int64sort
            floatType -> floatSort
            doubleType -> doubleSort
            else -> addressSort
        }
    }

    fun arrayDescriptorOf(type: IlArrayType): IlType {
        val elemType = type.elementType
        return if (isPrimitiveType(elemType) || elemType is IlStructType) elemType
        else objectType
    }

    // TODO need jacodb api for creating array types from element types
    fun arrayTypeOf(elemType: IlType): IlArrayType {
        val asmName = elemType.asmName
        return findTypeOrReportAbsence("${elemType.fullname}[]", asmName, useSystemPrefix = false) as IlArrayType
    }

    val indexOutOfRangeException: IlType by lazy { findTypeOrReportAbsence(indexOutOfRangeExceptionName, mscorelib) }
    val nullReferenceException: IlType by lazy { findTypeOrReportAbsence(nullReferenceExceptionName, mscorelib) }
    val invalidCastException: IlType by lazy { findTypeOrReportAbsence(invalidCastExceptionName, mscorelib) }
    val overflowException: IlType by lazy { findTypeOrReportAbsence(overflowExceptionName, mscorelib) }
    val divideByZeroException: IlType by lazy { findTypeOrReportAbsence(divideByZeroExceptionName, mscorelib) }
    val argumentException: IlType by lazy { findTypeOrReportAbsence(argumentExceptionName, mscorelib) }
    val argumentOutOfRangeException: IlType by lazy { findTypeOrReportAbsence(argumentOutOfRangeExceptionName, mscorelib) }
    val arrayTypeMismatchException: IlType by lazy { findTypeOrReportAbsence(arrayTypeMismatchExceptionName, mscorelib) }
    val argumentNullException: IlType by lazy { findTypeOrReportAbsence(argumentNullExceptionName, mscorelib)}

    private fun findTypeOrReportAbsence(typeName: String, asmName: String, useSystemPrefix: Boolean = true): IlType {
        val name = if (useSystemPrefix) "${SYSTEM_PREFIX}${typeName}" else typeName
        val typeId = TypeId(typeName = name, asmName = asmName, typeArgs = emptyList())
        return publication.findIlTypeOrNull(typeId) ?: error("$typeName was not found in publication")
    }
    // TODO fix
    fun isPrimitiveType(type: IlType): Boolean =
        when (type) {
            is IlEnumType -> true
            boolType -> true
            uint8Type -> true
            int8Type -> true
            int16Type -> true
            uint32Type -> true
            int32Type -> true
            uint32Type -> true
            int64Type -> true
            uint64Type -> true
            floatType -> true
            doubleType -> true
            else -> false
        }

    fun UExpr<UAddressSort>.toNumeric() : UExpr<UBvSort> =
        when (this) {
            is IlPtr -> this.toNumeric()
            else -> error("can not convert $this to numeric")
        }

    fun lValueLocation(lvalue: ULValue<*, *>, lvalueType: IlType): IlType =
        when (lvalue) {
            is UArrayIndexLValue<*, *, *> -> {
                val elemType = lvalue.arrayType as IlType
                arrayTypeOf(elemType)
            }
            is UFieldLValue<*, *> -> {
                val field = lvalue.field as IlField
                field.declaringType
            }
            is URegisterStackLValue<*> -> lvalueType

            else -> error("Unexpected lvalue $lvalue")
        }

}

private val SYSTEM_PREFIX = "System."
private const val indexOutOfRangeExceptionName = "IndexOutOfRangeException"
private const val nullReferenceExceptionName = "NullReferenceException"
private const val invalidCastExceptionName = "InvalidCastException"
private const val overflowExceptionName = "OverflowException"
private const val divideByZeroExceptionName = "DivideByZeroException"
private const val argumentExceptionName = "ArgumentException"
private const val arrayTypeMismatchExceptionName = "ArrayTypeMismatchException"
private const val argumentNullExceptionName = "ArgumentNullException"
private const val argumentOutOfRangeExceptionName = "ArgumentOutOfRangeException"
