package common

import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType

interface DecoderApi<T> {
    fun createBoolConst(value: Boolean): T
    fun createCharConst(value: Char): T
    fun createInt8Const(value: Byte): T
    fun createInt16Const(value: Short): T
    fun createInt32Const(value: Int): T
    fun createInt64Const(value: Long): T
    fun createUInt8Const(value: UByte): T
    fun createUInt16Const(value: UShort): T
    fun createUInt32Const(value: UInt): T
    fun createUInt64Const(value: ULong): T
    fun createFloatConst(value: Float): T
    fun createDoubleConst(value: Double): T
    fun createStringConst(value: String): T
    fun createNullConst(type: IlType): T

    fun createSlice(expr: T, start: Int, end: Int, pos: Int): T
    fun createCombine(slices: List<T>, sightType: IlType): T

    fun createArray(elementType: IlType, size: Int, address: Int): T
    fun createObject(type: IlType, address: Int): T

    fun setArrayIndex(array: T, index: Int, value: T)
    fun setObjectField(obj: T, field: IlField, value: T)

    fun callMethod(method: IlMethod, args: List<T>): T

}
