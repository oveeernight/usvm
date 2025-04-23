package samples

import IlMethodTestRunner
import org.junit.jupiter.api.TestInstance
import kotlin.test.Ignore
import kotlin.test.Test

class Unsafe : IlMethodTestRunner() {

    @Test
    fun argumentWrite() {
        runnerWithDefaultOptions(::argumentWrite)
    }
    @Test
    fun concreteUnsafe1() {
        runnerWithDefaultOptions(::concreteUnsafe1)
    }

    @Test
    fun stackUnsafe1() {
        runnerWithDefaultOptions(::stackUnsafe1)
    }

    @Test
    fun stackUnsafe2() {
        runnerWithDefaultOptions(::stackUnsafe2)
    }

    @Ignore("")
    @Test
    fun detachedPtr() {
        runnerWithDefaultOptions(::detachedPtr)
    }

    @Test
    fun concreteArrayUnsafe1() {
        runnerWithDefaultOptions(::concreteArrayUnsafe1)
    }
    @Test
    fun concreteArrayUnsafe2() {
        runnerWithDefaultOptions(::concreteArrayUnsafe2)
    }

    @Test
    fun symbolicArrayUnsafe1() {
        runnerWithDefaultOptions(::symbolicArrayUnsafe1)
    }

    @Test
    fun refField() {
        runnerWithDefaultOptions(::refField)
    }

    @Test
    fun concreteStructWrite() {
        runnerWithDefaultOptions(::concreteStructWrite)
    }

    @Test
    fun symbolicStructWrite() {
        runnerWithDefaultOptions(::symbolicStructWrite)
    }
}
