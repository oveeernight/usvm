package org.usvm.machine.interpreter

import org.example.ilinstances.IlMethod
import org.example.ilinstances.IlType
import org.jacodb.api.net.core.IlStmtVisitor
import org.jacodb.api.net.ilinstances.IlExpr
import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.UExpr
import org.usvm.USort

sealed interface IlTransparentStatement : IlStmt {
    val originalStmt: IlStmt
}

sealed interface IlMethodCallBaseStmt : IlTransparentStatement {
    val method: IlMethod

    override fun <T> accept(visitor: IlStmtVisitor<T>): T {
        error("Unexpected call on transparent statement $this")
    }
}



data class IlMethodEntryPointStmt(
    override val method: IlMethod,
    override val originalStmt: IlStmt,
    val entrypointArgs: List<Pair<IlType, UExpr<out USort>>>
) : IlMethodCallBaseStmt


interface IlMethodCall {
    val method: IlMethod
    val args: List<UExpr<out USort>>
    val returnSite: IlStmt
}
