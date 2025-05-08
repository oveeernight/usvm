package org.usvm.machine.interpreter

import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UConcreteHeapRef
import org.usvm.UExpr
import org.usvm.USort



sealed interface IlMethodResult {
    object BeforeCall : IlMethodResult

    class Success(val result: UExpr<out USort>, val method: IlMethod) : IlMethodResult

    class Exception(
        val ref: UConcreteHeapRef,
        val type: IlType,
        val method: IlMethod,
        val stmt: IlStmt
    ) : IlMethodResult
}
