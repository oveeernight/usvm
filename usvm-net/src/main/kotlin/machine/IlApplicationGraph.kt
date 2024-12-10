package org.usvm.machine

import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.statistics.ApplicationGraph

class IlApplicationGraph(val publication: IlPublication) : ApplicationGraph<IlMethod, IlStmt> {
    override fun predecessors(node: IlStmt): Sequence<IlStmt> {
        TODO("Not yet implemented")
    }

    override fun successors(node: IlStmt): Sequence<IlStmt> {
        TODO("Not yet implemented")
    }

    override fun callees(node: IlStmt): Sequence<IlMethod> {
        TODO("Not yet implemented")
    }

    override fun callers(method: IlMethod): Sequence<IlStmt> {
        TODO("Not yet implemented")
    }

    override fun entryPoints(method: IlMethod): Sequence<IlStmt> {
        TODO("Not yet implemented")
    }

    override fun exitPoints(method: IlMethod): Sequence<IlStmt> {
        TODO("Not yet implemented")
    }

    override fun methodOf(node: IlStmt): IlMethod {
        TODO("Not yet implemented")
    }

    override fun statementsOf(method: IlMethod): Sequence<IlStmt> {
        TODO("Not yet implemented")
    }
}
