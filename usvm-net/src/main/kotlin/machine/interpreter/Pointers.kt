package org.usvm.machine.interpreter

import org.usvm.*
import org.usvm.machine.IlContext
import org.usvm.machine.IlPtr


fun <Sort: USort> IlContext.shiftPointer(pointer: IlPtr<Sort>, shift: UExpr<UBvSort>): IlPtr<Sort> {
    val sightTypeSize : UExpr<UBvSort> = mkBv(pointer.sightType.size, bv32Sort)
    val newOffset = mkBvAddExpr(pointer.offset, mkBvMulExpr(shift, sightTypeSize))
    return mkPtr(pointer.location, newOffset, pointer.sightType)
}

fun IlPtr<*>.toNumeric() : UExpr<UBvSort> = offset
