package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Boxing: IlMethodTestRunner() {
    @Test
    fun boxInt() {
        runnerWithDefaultOptions(::boxInt)
    }

    @Test
    fun boxNullable1() {
        runnerWithDefaultOptions(::boxNullable1)
    }

    @Test
    fun unboxNullable() {
        runnerWithDefaultOptions(::unboxNullable)
    }

    @Test
    fun boxStruct() {
        runnerWithDefaultOptions(::boxStruct)
    }

    @Test
    fun interfacesArray() {
        runnerWithDefaultOptions(::interfacesArray)
    }

    @Test
    fun unboxInterface() {
        runnerWithDefaultOptions(::unboxInterface)
    }


    @Test
    fun unboxAny2() {
        runnerWithDefaultOptions(::unboxAny2)
    }

    @Test
    fun unboxAny3() {
        runnerWithDefaultOptions(::unboxAny3)
    }

    @Test
    fun unboxAny4() {
        runnerWithDefaultOptions(::unboxAny4)
    }

    @Test
    fun unboxAny5() {
        runnerWithDefaultOptions(::unboxAny5)
    }

    @Test
    fun unboxAny6() {
        runnerWithDefaultOptions(::unboxAny6)
    }

    @Test
    fun trickyBox() {
        runnerWithDefaultOptions(::trickyBox)
    }

    @Test
    fun box7() {
        runnerWithDefaultOptions(::box7)
    }

    @Test
    fun boxNullable() {
        runnerWithDefaultOptions(::boxNullable)
    }

    @Test
    fun alwaysNull() {
        runnerWithDefaultOptions(::alwaysNull)
    }

    @Test
    fun true1() {
        runnerWithDefaultOptions(::true1)
    }

    @Test
    fun true2() {
        runnerWithDefaultOptions(::true2)
    }

    @Test
    fun true3() {
        runnerWithDefaultOptions(::true3)
    }
}
