package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Exceptions : IlMethodTestRunner() {
    @Test
    fun arrayIndexReading() {
        runnerWithDefaultOptions(::arrayIndexReading)
    }

    @Test
    fun filterScope() {
        runnerWithDefaultOptions(::filterScope)
    }

    @Test
    fun exceptionInFilterScope() {
        runnerWithDefaultOptions(::exceptionInFilterScope)
    }

    @Test
    fun exceptionFromCallee() {
        runnerWithDefaultOptions(::exceptionFromCallee)
    }

    @Test
    fun throwExceptionInCatch() {
        runnerWithDefaultOptions(::throwExceptionInCatch)
    }

    @Test
    fun nestedBlocks() {
        runnerWithDefaultOptions(::nestedBlocks)
    }

    @Test
    fun severalBlocks() {
        runnerWithDefaultOptions(::severalBlocks)
    }
}
