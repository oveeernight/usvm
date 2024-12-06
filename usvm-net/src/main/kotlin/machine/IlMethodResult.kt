package org.usvm.machine

import org.example.ilinstances.IlMethod
import org.example.ilinstances.IlType
import org.jacodb.api.net.ilinstances.IlExpr
import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.UConcreteHeapRef
import org.usvm.UExpr
import org.usvm.USort

sealed interface IlMethodResult {
    object NoCall : IlMethodResult

    class Success(val result: UExpr<out USort>, val method: IlMethod) : IlMethodResult

    class Exception(
        val exception: UConcreteHeapRef,
        val type: IlType,
        val method: IlMethod,
        val stmt: IlStmt
    ) : IlMethodResult
}
