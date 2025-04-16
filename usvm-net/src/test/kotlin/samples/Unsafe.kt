package samples

import IlMethodTestRunner
import org.junit.jupiter.api.TestInstance
import kotlin.test.Ignore
import kotlin.test.Test

class Unsafe : IlMethodTestRunner() {

    @Test
    fun argumentWrite() {
        runner(::argumentWrite, options)
    }
    @Test
    fun concreteUnsafe1() {
        runner(::concreteUnsafe1, options)
    }

    @Test
    fun stackUnsafe1() {
        runner(::stackUnsafe1, options)
    }

    @Test
    fun stackUnsafe2() {
        runner(::stackUnsafe2, options)
    }

    @Ignore("")
    @Test
    fun detachedPtr() {
        runner(::detachedPtr, options)
    }

    @Test
    fun concreteArrayUnsafe1() {
        runner(::concreteArrayUnsafe1, options)
    }
    @Test
    fun concreteArrayUnsafe2() {
        runner(::concreteArrayUnsafe2, options)
    }

    @Test
    fun symbolicArrayUnsafe1() {
        runner(::symbolicArrayUnsafe1, options)
    }

    @Test
    fun refField() {
        runner(::refField, options)
    }

    @Test
    fun concreteStructWrite() {
        runner(::concreteStructWrite, options)
    }

    @Test
    @Ignore
    fun symbolicStructWrite() {
        runner(::symbolicStructWrite, options)
    }
}
