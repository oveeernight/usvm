package samples

import IlMethodTestRunner
import kotlin.test.Test

class Unsafe : IlMethodTestRunner() {

    @Test
    fun concreteUnsafe1() {
        runner(::concreteUnsafe1, options)
    }

    @Test
    fun symbolicUnsafe1() {
        runner(::symbolicUnsafe1, options)
    }
}
