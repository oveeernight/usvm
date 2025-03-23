package org.usvm.machine.interpreter

import org.jacodb.api.common.CommonType
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.machine.IlContext
import org.usvm.machine.IlPtr
import org.usvm.machine.USizeSort
import org.usvm.machine.ilctx
import org.usvm.memory.ULValue


fun <Key, Sort: USort> IlContext.shiftPointer(pointer: IlPtr<Key, Sort>, shift: UExpr<UBvSort>): IlPtr<Key, Sort> {
    val rhsByteSize : UExpr<UBvSort> = mkBv((shift.sort.sizeBits / 8u).toInt(), bv32Sort)
    val newOffset = mkBvAddExpr(pointer.offset, mkBvMulExpr(shift, rhsByteSize))
    return mkPtr(pointer.base, newOffset, pointer.sightType)
}
