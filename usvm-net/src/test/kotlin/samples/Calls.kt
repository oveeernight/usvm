package samples

import IlMethodTestRunner
import kotlin.test.Test

class Calls : IlMethodTestRunner() {
    @Test
    fun mulOrAdd() {
        runnerWithDefaultOptions(::mulOrAdd)
    }


    @Test
    fun nonVirtualCall1() {
        runnerWithDefaultOptions(::nonVirtualCall1)
    }

    @Test
    fun virtualCall1() {
        runnerWithDefaultOptions(::virtualCall1)
    }

    @Test
    fun virtualCall2() {
        runnerWithDefaultOptions(::virtualCall2)
    }

    @Test
    fun virtualCallOnSymbolicReading() {
        runnerWithDefaultOptions(::virtualCallOnSymbolicReading)
    }
}
