package org.usvm.machine

import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.memory.ULValue
import org.usvm.memory.UWritableMemory

@Suppress("UNCHECKED_CAST")
fun UWritableMemory<IlType>.write(lvalue: ULValue<*, *>, rvalue: UExpr<out USort>) {
    write(lvalue as ULValue<*, USort>, rvalue as UExpr<USort>, guard = rvalue.ctx.trueExpr)
}
