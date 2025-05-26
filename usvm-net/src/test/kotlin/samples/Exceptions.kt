package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Exceptions : IlMethodTestRunner() {
    @Test
    fun symbolicDivision() {
        runnerWithDefaultOptions(::symbolicDivision)
    }

    @Test
    fun throwNpe() {
        runnerWithDefaultOptions(::throwNpe)
    }

    @Test
    fun catchRuntimeException() {
        runnerWithDefaultOptions(::catchRuntimeException)
    }

    @Test
    fun tryWith2Leaves() {
        runnerWithDefaultOptions(::tryWith2Leaves)
    }

    @Test
    fun arrayIndexReading() {
        runnerWithDefaultOptions(::arrayIndexReading)
    }

    @Test
    fun simpleFilterScope() {
        runnerWithDefaultOptions(::simpleFilterScope)
    }

    // TODO endfilter tac errors
    @Test
    fun filterInsideFinally() {
        runnerWithDefaultOptions(::filterInsideFinally)
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

    @Test
    fun finallyChain() {
        runnerWithDefaultOptions(::finallyChain)
    }

    @Test
    fun finallyInCalleeExecutedWhenCaughtInCaller() {
        runnerWithDefaultOptions(::finallyInCalleeExecutedWhenCaughtInCaller)
    }

    @Test
    fun filterInCallerExecutedBeforeFinallyInCallee() {
        runnerWithDefaultOptions(::filterInCallerExecutedBeforeFinallyInCallee)
    }

    @Test
    fun filterThrowingException() {
        runnerWithDefaultOptions(::filterThrowingException)
    }

    @Test
    fun manyNestedTryBlocks() {
        runnerWithDefaultOptions(::manyNestedTryBlocks)
    }

    @Test
    fun callInsideFinally() {
        runnerWithDefaultOptions(::callInsideFinally)
    }
}
