package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Unsafe : IlMethodTestRunner() {

    @Test
    fun managedRef() {
        runner(::managedRef, options)
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
}
