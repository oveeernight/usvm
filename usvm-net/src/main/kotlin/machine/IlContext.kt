package org.usvm.machine

import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.IlFieldDto
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlArrayType
import org.jacodb.api.net.ilinstances.impl.IlFieldImpl
import org.jacodb.api.net.ilinstances.impl.IlReferenceType
import org.jacodb.api.net.ilinstances.impl.IlTypeImpl
import org.jacodb.api.net.publication.IlPredefinedTypesExt.int32
import org.usvm.UBv32Sort
import org.usvm.UContext
import org.usvm.USort

typealias USizeSort = UBv32Sort

class IlContext(val publication: IlPublication, components: IlComponents) : UContext<USizeSort>(components) {
    val boolType by lazy { findTypeOrReportAbsence("Bool") }
    val charType by lazy { findTypeOrReportAbsence("Char") }
    val int8Type by lazy { findTypeOrReportAbsence("SByte") }
    val int16Type by lazy {findTypeOrReportAbsence("Short") }
    val int32Type by lazy { findTypeOrReportAbsence("Int") }
    val int64Type by lazy { findTypeOrReportAbsence("Long") }
    val uint8Type by lazy { findTypeOrReportAbsence("Byte") }
    val uint16Type by lazy { findTypeOrReportAbsence("UShort") }
    val uint32Type by lazy { findTypeOrReportAbsence("UInt") }
    val uint64Type by lazy { findTypeOrReportAbsence("ULong") }
    val floatType by lazy { findTypeOrReportAbsence("Float") }
    val doubleType by lazy { findTypeOrReportAbsence("Double") }
    val stringType by lazy { findTypeOrReportAbsence("String") }

    val objectType by lazy { findTypeOrReportAbsence("Object") }
    val systemType by lazy { findTypeOrReportAbsence("Type") }

    val byteSort = bv8Sort
    val charSort = bv16Sort
    val longSort = bv64Sort
    val floatSort = fp32Sort
    val doubleSort = fp64Sort
    val voidSort by lazy { VoidSort(this) }
    val sizeSort = bv32Sort

    val byteBitSize = 8u
    val shortBitSize = 16u
    val intBitSize = 32u
    val longBitSize = 64u

    val void by lazy { VoidValue(this) }

    val mockType = findTypeOrReportAbsence("Mock")

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
        TODO()
        when (type) {
            is IlReferenceType -> addressSort
            // TODO predefined primitives
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

    val indexOutOfRangeException: IlType = findTypeOrReportAbsence(indexOutOfRangeExceptionName)
    val nullReferenceException: IlType = findTypeOrReportAbsence(nullReferenceExceptionName)
    val invalidCastException: IlType = findTypeOrReportAbsence(invalidCastExceptionName)

    private fun findTypeOrReportAbsence(typeName: String): IlType =
        publication.findIlTypeOrNull(typeName) ?: error("$typeName was not found in publication")

    private fun isPrimitiveType(type: IlType): Boolean = TODO()

}

const val indexOutOfRangeExceptionName = "System.IndexOutOfRangeException"
const val nullReferenceExceptionName = "System.NullReferenceException"
const val invalidCastExceptionName = "System.InvalidCastException"
