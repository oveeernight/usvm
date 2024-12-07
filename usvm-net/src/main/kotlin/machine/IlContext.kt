package org.usvm.machine

import org.example.ilinstances.IlArrayType
import org.example.ilinstances.IlClassType
import org.example.ilinstances.IlReferenceType
import org.example.ilinstances.IlType
import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.IlFieldDto
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlArrayAccess
import org.jacodb.api.net.ilinstances.IlField
import org.usvm.UBv32Sort
import org.usvm.UContext
import org.usvm.UNullRef
import org.usvm.USort

typealias USizeSort = UBv32Sort

class IlContext(private val publication: IlPublication, components: IlComponents) : UContext<USizeSort>(components) {
    val byteSort = bv8Sort
    val charSort = bv16Sort
    val longSort = bv64Sort
    val floatSort = fp32Sort
    val doubleSort = fp64Sort
    val voidSort by lazy { VoidSort(this) }

    val sizeSort = bv32Sort

    val bytesBitSize = 8u
    val shortBitSize = 16u
    val intBitSize = 32u
    val intLongSize = 64u

    val void by lazy { VoidValue(this) }

    val mockType = findTypeOrReportAbsence("Mock")

    val systemType by lazy { findTypeOrReportAbsence("System.Type")}

    val syntheticTypeField : IlField by lazy {
        val dto = IlFieldDto(
            fieldType = TypeId(asmName = "mscorlib", typeName = "Type"),
            isStatic = false,
            name = "type",
            attrs = emptyList()
        )

        IlField(systemType, dto, publication)
    }

    fun typeToSort(type: IlType): USort {
        TODO()
        when (type) {
            is IlReferenceType -> addressSort
            // TODO predefined primitives
        }
    }

    val charType = findTypeOrReportAbsence("Char")

    fun arrayDescriptorOf(type: IlArrayType): IlType {
        TODO()
    }

    fun arrayTypeOf(elemType: IlType): IlArrayType {
        TODO()
    }

    val indexOutOfRangeException: IlType = findTypeOrReportAbsence(indexOutOfRangeExceptionName)
    val nullReferenceException: IlType = findTypeOrReportAbsence(nullReferenceExceptionName)
    val invalidCastException: IlType = findTypeOrReportAbsence(invalidCastExceptionName)

    private fun findTypeOrReportAbsence(typeName: String): IlType =
        publication.findIlTypeOrNull(typeName) ?: error("$typeName was not found in publication")

}

const val indexOutOfRangeExceptionName = "System.IndexOutOfRangeException"
const val nullReferenceExceptionName = "System.NullReferenceException"
const val invalidCastExceptionName = "System.InvalidCastException"
