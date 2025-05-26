package org.usvm.machine

import mu.KLogging
import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.ilinstances.IlMethod
import org.usvm.UInterpreter
import org.usvm.UMachine
import org.usvm.UMachineOptions
import org.usvm.UPathSelector
import org.usvm.forkblacklists.UForkBlackList
import org.usvm.machine.interpreter.IlInterpreter
import org.usvm.machine.interpreter.IlMethodResult
import org.usvm.ps.createPathSelector
import org.usvm.machine.state.IlState
import org.usvm.statistics.TimeStatistics
import org.usvm.statistics.UMachineObserver
import org.usvm.statistics.collectors.AllStatesCollector
import org.usvm.stopstrategies.StopStrategy
import org.usvm.util.bracket
import org.usvm.util.debug
import org.usvm.utils.isSat

val logger = object : KLogging() {}.logger

class IlMachine(
    publication: IlPublication,
    private val options: UMachineOptions,
    private val ilOptions: IlMachineOptions
) : UMachine<IlState>() {
    private val typeSystem = IlTypeSystem(publication)
    private val components = IlComponents(typeSystem, options)
    private val ctx = IlContext(publication, components)
    private val applicationGraph = IlApplicationGraph()
    private val interpreter = IlInterpreter(ctx, applicationGraph,  ilOptions, UForkBlackList.createDefault())


    override fun run(
        interpreter: UInterpreter<IlState>,
        pathSelector: UPathSelector<IlState>,
        observer: UMachineObserver<IlState>,
        isStateTerminated: (IlState) -> Boolean,
        stopStrategy: StopStrategy
    ) {
        org.usvm.logger.debug().bracket("$this.run($interpreter, ${pathSelector::class.simpleName})") {
            observer.onMachineStarted()
            try {
                while (!pathSelector.isEmpty() && !stopStrategy.shouldStop()) {
                    val state = pathSelector.peek()
                    observer.onStatePeeked(state)

                    var (forkedStates: Sequence<IlState>, stateAlive) = emptySequence<IlState>() to false
                    try {
                        val (s, a) = interpreter.step(state)
                        forkedStates = s
                        stateAlive = a
                    }
                    catch (e: Exception) {
                        forkedStates = emptySequence()
                        stateAlive = false
                    }
                    observer.onState(state, forkedStates)

                    val originalStateAlive = stateAlive && !isStateTerminated(state)
                    val aliveForkedStates = mutableListOf<IlState>()
                    for (forkedState in forkedStates) {
                        if (!isStateTerminated(forkedState)) {
                            aliveForkedStates.add(forkedState)
                        } else {
                            // TODO: distinguish between states terminated by exception (runtime or user) and
                            //  those which just exited
                            if (forkedState.isSat()) {
                                observer.onStateTerminated(forkedState, stateReachable = true)
                            }
                        }
                    }

                    if (originalStateAlive) {
                        pathSelector.update(state)
                    } else {
                        pathSelector.remove(state)
                        if (state.isSat()) {
                            observer.onStateTerminated(state, stateReachable = stateAlive)
                        }
                    }

                    if (aliveForkedStates.isNotEmpty()) {
                        pathSelector.add(aliveForkedStates)
                    }
                }
            } finally {
                observer.onMachineStopped()
            }

            if (!pathSelector.isEmpty()) {
                org.usvm.logger.debug { stopStrategy.stopReason() }
            }
        }
    }

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


    private fun isStateTerminated(state: IlState) =
        state.callStack.isEmpty() || state.criticalErrorOccurred

    override fun close() {

    }
}

class IlMachineOptions(
    val forkOnRemainingTypes: Boolean = false,
    val maxArraySize: Int = 100,
    val forkOnImplicitExceptions: Boolean = true,
)
