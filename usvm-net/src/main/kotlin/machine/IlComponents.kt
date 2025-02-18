package org.usvm.machine

import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UBv32Sort
import org.usvm.UComponents
import org.usvm.UContext
import org.usvm.USizeExprProvider
import org.usvm.solver.USolverBase
import org.usvm.types.UTypeSystem

class IlComponents : UComponents<IlType, UBv32Sort> {
    override val useSolverForForks: Boolean
        get() = TODO("Not yet implemented")

    override fun <Context : UContext<UBv32Sort>> mkSizeExprProvider(ctx: Context): USizeExprProvider<UBv32Sort> {
        TODO("Not yet implemented")
    }

    override fun mkTypeSystem(ctx: UContext<UBv32Sort>): UTypeSystem<IlType> {
        TODO("Not yet implemented")
    }

    override fun <Context : UContext<UBv32Sort>> mkSolver(ctx: Context): USolverBase<IlType> {
        TODO("Not yet implemented")
    }
}
