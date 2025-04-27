package samples

import IlMethodTestRunner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Ignore
import kotlin.test.Test

class Arrays : IlMethodTestRunner() {
    @Test
    fun arrayStore() {
        runner(::arrayStore, options)
    }

    @Test
    fun arraySimpleBranch() {
        runner(::arraySimpleBranch, options)
    }

    @Test
    fun concreteArraySymbolicIndex() {
        runner(::concreteArraySymbolicIndex, options)
    }

    @Test
    fun classesArray() {
        runnerWithDefaultOptions(::classesArray)
    }

    @Ignore("unsafe")
    @Test
    fun stringIndex() {
        runner(::stringIndex, options)
    }
}
