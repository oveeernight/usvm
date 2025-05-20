package org.usvm.machine.interpreter

import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.IlContext
import org.usvm.machine.IlPtr


fun IlContext.shiftPointer(pointer: IlPtr, shiftInBytes: UExpr<UBvSort>): IlPtr {
    val newOffset = mkBvAddExpr(pointer.offset, shiftInBytes)
    return mkPtr(pointer.base, pointer.baseType,  newOffset, pointer.sightType)
}

fun IlContext.addPointers(lhs: IlPtr, rhs: IlPtr): IlPtr {
    assert(rhs.base == null)
    val newOffset = mkBvAddExpr(lhs.offset, rhs.offset)
    return mkPtr(lhs.base, lhs.baseType,  newOffset, lhs.sightType)
}

fun IlContext.mulOffset(ptr: IlPtr, m: UExpr<UBvSort>): IlPtr {
    val newOffset = mkBvMulExpr(ptr.offset, m)
    return mkPtr(ptr.base, ptr.baseType,  newOffset,  ptr.sightType)
}

fun IlContext.mkDetachedPtr(offset: UExpr<UBvSort>, pointedType: IlType): IlPtr {
    return mkPtr(null, pointedType, offset, pointedType)
}

fun IlPtr.toNumeric() : UExpr<UBvSort> = offset
