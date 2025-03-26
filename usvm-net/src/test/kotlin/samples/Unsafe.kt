package samples

import IlMethodTestRunner
import kotlin.test.Test

class Unsafe : IlMethodTestRunner() {

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
}
