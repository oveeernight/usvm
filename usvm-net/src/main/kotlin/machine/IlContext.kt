package org.usvm.machine

import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.IlFieldDto
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlArrayType
import org.jacodb.api.net.ilinstances.impl.IlFieldImpl
import org.jacodb.api.net.ilinstances.impl.IlTypeImpl
import org.usvm.UBv32Sort
import org.usvm.UContext
import org.usvm.USort

typealias USizeSort = UBv32Sort

class IlContext(val publication: IlPublication, components: IlComponents) : UContext<USizeSort>(components) {
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

    // TODO should be removed
//    val mockType = findTypeOrReportAbsence("Mock")

    val syntheticTypeField : IlField by lazy {
        val dto = IlFieldDto(
            fieldType = TypeId(asmName = "mscorlib", typeName = "Type"),
            isStatic = false,
            name = "type",
            attrs = emptyList()
        )

        IlFieldImpl(systemType as IlTypeImpl, dto, publication)
    }

    fun typeToSort(type: IlType): USort {
        // TODO unsigned
        return when (type) {
            boolType -> boolSort
            charType -> charSort
//            int8Type -> byteSort
            int16Type -> int16sort
            int32Type -> sizeSort
            int64Type -> int64sort
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

    private fun findTypeOrReportAbsence(typeName: String): IlType =
        publication.findIlTypeOrNull("${SYSTEM_PREFIX}${typeName}") ?: error("$typeName was not found in publication")

    // TODO fix
    fun isPrimitiveType(type: IlType): Boolean =
        type == uint8Type || type == int32Type || type == int64Type || type == charType || type == boolType

}

private val SYSTEM_PREFIX = "System."
const val indexOutOfRangeExceptionName = "IndexOutOfRangeException"
const val nullReferenceExceptionName = "NullReferenceException"
const val invalidCastExceptionName = "InvalidCastException"
