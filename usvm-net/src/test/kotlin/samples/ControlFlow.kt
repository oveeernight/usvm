package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test


class ControlFlow: IlMethodTestRunner() {
    @Test
    fun switchWithSequentialCases() {
        runnerWithDefaultOptions(::switchWithSequentialCases)
    }

    @Ignore
    @Test
    fun gotos1() {
        runnerWithDefaultOptions(::gotos1)
    }

    @Ignore
    @Test
    fun gotos2() {
        runnerWithDefaultOptions(::gotos2)
    }

    @Ignore("too long")
    @Test
    fun gotosWithinSwitch() {
        runnerWithDefaultOptions(::gotosWithinSwitch)
    }

    @Test
    fun acyclicGotos() {
        runnerWithDefaultOptions(::acyclicGotos)
    }

    @Test
    fun sequentialIfsHard() {
        runnerWithDefaultOptions(::sequentialIfsHard)
    }

    @Test
    fun sequentialIfsSimple() {
        runnerWithDefaultOptions(::sequentialIfsSimple)
    }

    @Ignore("track loops")
    @Test
    fun binarySearch() {
        runnerWithDefaultOptions(::binarySearch)
    }

    @Ignore("track loops")
    @Test
    fun cycleWith3EntryPoints() {
        runnerWithDefaultOptions(::cycleWith3EntryPoints)
    }
}
