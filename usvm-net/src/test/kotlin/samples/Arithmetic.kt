package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Arithmetic : IlMethodTestRunner() {
    @Test
    fun add() {
        runner(::add, options)
    }

    @Test
    fun subtract() {
        runner(::subtract, options)
    }

    @Test
    fun multiply() {
        runner(::multiply, options)
    }

    @Test
    fun divide() {
        runner(::divide, options)
    }

    @Test
    fun modulo() {
        runner(::modulo, options)
    }

    @Test
    fun gt() {
        runner(::gt, options)
    }

    @Test
    fun ge() {
        runner(::ge, options)
    }

    @Test
    fun lt() {
        runner(::lt, options)
    }

    @Ignore("Wait for binop types unification")

    @Test
    fun le() {
        runner(::le, options)
    }


    @Test
    fun shl() {
        runner(::shl, options)
    }

    @Test
    fun shr() {
        runner(::shr, options)
    }
}
