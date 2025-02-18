package org.usvm.machine.interpreter

import org.jacodb.api.net.core.IlStmtVisitor
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UExpr
import org.usvm.UHeapRef
import org.usvm.USort

sealed interface IlTransparentStatement : IlStmt {
    val originalStmt: IlStmt
}

interface TransparentMethodCallBaseStmt : IlTransparentStatement{
    val method: IlMethod
}


interface MethodCall {
    val method: IlMethod
    val args: List<UExpr<out USort>>
    val returnSite: IlStmt
}



data class IlMethodEntryPointStmt(
    override val method: IlMethod,
    val refArgs: List<Pair<IlType, UHeapRef>>
) : TransparentMethodCallBaseStmt {
    override val originalStmt = TODO()
//    method.rawInstList.first()

    override fun <T> accept(visitor: IlStmtVisitor<T>): T {
        error("IlMethodEntryPointStmt: visitor should not be called on transparent instructions")
    }
}

data class IlConcreteCallStmt(
    override val method: IlMethod,
    override val args: List<UExpr<out USort>>,
    override val returnSite: IlStmt,
) : MethodCall, TransparentMethodCallBaseStmt {
    override val originalStmt = returnSite
    override fun <T> accept(visitor: IlStmtVisitor<T>): T {
        error("IlConcreteCall: visitor should not be called on transparent instructions")
    }
}
