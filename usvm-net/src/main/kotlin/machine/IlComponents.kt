package org.usvm.machine

import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.memory.UReadOnlyMemory
import org.usvm.model.ULazyModelDecoder
import org.usvm.solver.UExprTranslator
import org.usvm.solver.USolverBase
import org.usvm.solver.UTypeSolver
import org.usvm.types.UTypeSystem

class IlComponents(private val typeSystem: IlTypeSystem, private val options: UMachineOptions) :
    UComponents<IlType, UBv32Sort> {

    private val closeableResources = mutableListOf<AutoCloseable>()

    override val useSolverForForks: Boolean = options.useSolverForForks

    override fun <Context : UContext<UBv32Sort>> mkSizeExprProvider(ctx: Context): USizeExprProvider<UBv32Sort> {
        return UBv32SizeExprProvider(ctx)
    }

    override fun mkTypeSystem(ctx: UContext<UBv32Sort>): UTypeSystem<IlType> {
        return typeSystem
    }

    override fun <Context : UContext<UBv32Sort>> mkComposer(ctx: Context): (UReadOnlyMemory<IlType>, MutabilityOwnership) -> UComposer<IlType, UBv32Sort> =
        { memory, ownership -> IlComposer(ctx, memory, ownership) }

    override fun <Context : UContext<UBv32Sort>> buildTranslatorAndLazyDecoder(ctx: Context): Pair<UExprTranslator<IlType, UBv32Sort>, ULazyModelDecoder<IlType>> {
        val translator = IlTranslator(ctx)
        val decoder : ULazyModelDecoder<IlType> = ULazyModelDecoder(translator)
        return translator to decoder
    }

    override fun <Context : UContext<UBv32Sort>> mkSolver(ctx: Context): USolverBase<IlType> {
        val (translator, decoder) = buildTranslatorAndLazyDecoder(ctx)
        val solverFactory = SolverFactory.mkFactory(options.runSolverInAnotherProcess)
        val solver = solverFactory.mkSolver(ctx, options.solverType)
        val typeSolver = UTypeSolver(typeSystem)
        closeableResources += solver
        closeableResources += solverFactory

        return USolverBase(ctx, solver, typeSolver, translator, decoder, options.solverTimeout)
    }
}
