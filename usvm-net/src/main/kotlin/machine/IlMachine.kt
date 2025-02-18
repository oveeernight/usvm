package org.usvm.machine

import org.example.ilinstances.IlMethod
import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.UMachine
import org.usvm.UMachineOptions
import org.usvm.machine.interpreter.IlInterpreter
import org.usvm.ps.createPathSelector
import org.usvm.machine.state.IlState
import org.usvm.statistics.ApplicationGraph
import org.usvm.statistics.TimeStatistics
import org.usvm.statistics.collectors.AllStatesCollector

class IlMachine(
    publication: IlPublication,
    private val options: UMachineOptions,
    private val ilOptions: IlMachineOptions) : UMachine<IlState>() {
    private val components = IlComponents()
    private val ctx = IlContext(publication, components)
    private val interpreter = IlInterpreter(ctx)
    private val applicationGraph = IlApplicationGraph(publication)

    fun analyze(methods: List<IlMethod>): List<IlState> {
        val initialStates = mutableMapOf<IlMethod, IlState>()
        methods.forEach { method ->
            initialStates[method] = interpreter.getInitialState(method)
        }

        val timeStatistics = TimeStatistics<IlMethod, IlState>()
        val allStatesObserver = AllStatesCollector<IlState>()
        val ps = createPathSelector(initialStates, options, applicationGraph, timeStatistics)

        run(interpreter, ps, observer = allStatesObserver, isStateTerminated = ::isStateTerminated)

        return allStatesObserver.collectedStates
    }


    private fun isStateTerminated(state: IlState) = state.callStack.isEmpty()

    override fun close() {
        TODO()
    }
}

class IlMachineOptions {
    val maxArraySize = 1_000
    val forkOnImplicitExceptions = true
}
