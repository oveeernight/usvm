package org.usvm.machine

import org.jacodb.api.net.cfg.IlGraphImpl
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.statistics.ApplicationGraph

class IlApplicationGraph: ApplicationGraph<IlMethod, IlStmt> {
    private val cfgCache = mutableMapOf<IlMethod, IlGraphImpl>()

    override fun predecessors(node: IlStmt): Sequence<IlStmt> {
        return node.method.getCfg().predecessors(node).asSequence()
    }

    override fun successors(node: IlStmt): Sequence<IlStmt> {
        return node.method.getCfg().successors(node).asSequence()
    }

    override fun callees(node: IlStmt): Sequence<IlMethod> {
        TODO("Usages feature")
    }

    override fun callers(method: IlMethod): Sequence<IlStmt> {
        TODO("Usages feature")
    }

    override fun entryPoints(method: IlMethod): Sequence<IlStmt> {
        return method.getCfg().entries.asSequence()
    }

    override fun exitPoints(method: IlMethod): Sequence<IlStmt> {
        TODO("Not yet implemented")
    }

    override fun methodOf(node: IlStmt): IlMethod {
        return node.method
    }

    override fun statementsOf(method: IlMethod): Sequence<IlStmt> {
        return method.instList.asSequence()
    }

    private fun IlMethod.getCfg() : IlGraphImpl = cfgCache.getOrPut(this) {
        IlGraphImpl(this, instList)
    }
}
