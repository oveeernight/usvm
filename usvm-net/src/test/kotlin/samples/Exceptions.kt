package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Exceptions : IlMethodTestRunner() {
    @Test
    fun arrayIndexReading() {
        runnerWithDefaultOptions(::arrayIndexReading)
    }

    @Ignore("fix tac")
    @Test
    fun simpleFilterScope() {
        runnerWithDefaultOptions(::simpleFilterScope)
    }

    @Ignore("fix tac")
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
}
