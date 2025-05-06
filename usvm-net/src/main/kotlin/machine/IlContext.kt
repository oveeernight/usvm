package org.usvm.machine

import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.IlFieldDto
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.*
import org.jacodb.api.net.publication.IlPredefinedAsmExt.mscorelib
import org.usvm.*
import org.usvm.machine.state.boxed.IlInputBoxedValuesId
import org.usvm.machine.state.IlStaticFieldsRegionId
import org.usvm.memory.ULValue
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

    fun <Sort : USort> mkPtr(
        base: ULValue<*, Sort>,
        baseType: IlType,
        locationType: IlType,
        offset: UExpr<UBvSort>,
        sightType: IlType
    ): IlPtr<Sort> = IlPtr(this, base, baseType, locationType, offset, sightType)

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
//            int8Type -> byteSort
            int16Type -> int16sort
            int32Type -> sizeSort
            int64Type -> int64sort
            uint32Type -> int32sort
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

    private fun findTypeOrReportAbsence(typeName: String, asmName: String, useSystemPrefix: Boolean = true): IlType {
        val name = if (useSystemPrefix) "${SYSTEM_PREFIX}${typeName}" else typeName
        val typeId = TypeId(typeName = name, asmName = asmName, typeArgs = emptyList())
        return publication.findIlTypeOrNull(typeId) ?: error("$typeName was not found in publication")
    }
    // TODO fix
    fun isPrimitiveType(type: IlType): Boolean =
        type is IlEnumType || type == uint8Type || type == int32Type || type == int64Type || type == charType || type == boolType

    fun UExpr<UAddressSort>.toNumeric() : UExpr<UBvSort> =
        when (this) {
            is IlPtr<*> -> this.toNumeric()
            else -> error("can not convert $this to numeric")
        }
}

private val SYSTEM_PREFIX = "System."
const val indexOutOfRangeExceptionName = "IndexOutOfRangeException"
const val nullReferenceExceptionName = "NullReferenceException"
const val invalidCastExceptionName = "InvalidCastException"
