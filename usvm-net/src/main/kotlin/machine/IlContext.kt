package org.usvm.machine

import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.IlFieldDto
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlArrayType
import org.jacodb.api.net.ilinstances.impl.IlFieldImpl
import org.jacodb.api.net.ilinstances.impl.IlTypeImpl
import org.jacodb.api.net.publication.IlPredefinedAsmExt.mscorelib
import org.usvm.*
import org.usvm.memory.ULValue

typealias USizeSort = UBv32Sort

class IlContext(val publication: IlPublication, components: IlComponents) : UContext<USizeSort>(components) {
    //
    private val mscorelib by lazy { publication.mscorelib() }
    val boolType by lazy { findTypeOrReportAbsence("Boolean") }
    val charType by lazy { findTypeOrReportAbsence("Char") }
    val int8Type by lazy { findTypeOrReportAbsence("SByte") }
    val int16Type by lazy { findTypeOrReportAbsence("Int16") }
    val int32Type by lazy { findTypeOrReportAbsence("Int32") }
    val int64Type by lazy { findTypeOrReportAbsence("Int64") }
    val uint8Type by lazy { findTypeOrReportAbsence("Byte") }
    val uint16Type by lazy { findTypeOrReportAbsence("UInt16") }
    val uint32Type by lazy { findTypeOrReportAbsence("UInt32") }
    val uint64Type by lazy { findTypeOrReportAbsence("UInt64") }
    val floatType by lazy { findTypeOrReportAbsence("Float") }
    val doubleType by lazy { findTypeOrReportAbsence("Double") }
    val stringType by lazy { findTypeOrReportAbsence("String") }

    val objectType by lazy { findTypeOrReportAbsence("Object") }
    val systemType by lazy { findTypeOrReportAbsence("Type") }

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

    fun <Key, Sort: USort> mkManagedRef(key: ULValue<Key, Sort>, type: IlType) : IlManagedRef<Key, Sort> =
        IlManagedRef(this, type, key)

    fun <Sort : USort> mkPtr(
        base: ULValue<*, Sort>,
        offset: UExpr<UBvSort>,
        sightType: IlType
    ): IlPtr<Sort> = IlPtr(this, base, offset, sightType)

//    fun mkDetachedPtr(offset: UExpr<UBvSort>, sightType: IlType): IlPtr<UAddressSort> {
//        val location = IlHeapLocation(nullRef, addressSort, sightType, isArray = false)
//        return IlPtr(this, location, offset, sightType)
//    }

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

    fun typeToSort(type: IlType): USort {
        // TODO unsigned
        return when (type.fullname) {
            boolType.fullname -> boolSort
            charType.fullname -> charSort
//            int8Type -> byteSort
            int16Type.fullname -> int16sort
            int32Type.fullname -> sizeSort
            int64Type.fullname -> int64sort
            else -> addressSort
        }
    }

    fun arrayDescriptorOf(type: IlArrayType): IlType {
        return if (isPrimitiveType(type.elementType)) {
            type.elementType
        } else {
            objectType
        }
    }

    fun arrayTypeOf(elemType: IlType): IlArrayType {
        // TODO need jacodb api for creating array types from element types
        return findTypeOrReportAbsence("${elemType.fullname}[]") as IlArrayType
    }

    val indexOutOfRangeException: IlType by lazy { findTypeOrReportAbsence(indexOutOfRangeExceptionName) }
    val nullReferenceException: IlType by lazy { findTypeOrReportAbsence(nullReferenceExceptionName) }
    val invalidCastException: IlType by lazy { findTypeOrReportAbsence(invalidCastExceptionName) }

    private fun findTypeOrReportAbsence(typeName: String): IlType {
        val typeId = TypeId(typeName = "${SYSTEM_PREFIX}${typeName}", asmName = mscorelib, typeArgs = emptyList())
        return publication.findIlTypeOrNull(typeId) ?: error("$typeName was not found in publication")
    }
    // TODO fix
    fun isPrimitiveType(type: IlType): Boolean =
        type == uint8Type || type == int32Type || type == int64Type || type == charType || type == boolType

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
