package samples

import IlMethodTestRunner
import kotlin.test.Test

class   Boxing: IlMethodTestRunner() {
    @Test
    fun boxInt() {
        runnerWithDefaultOptions(::boxInt)
    }

    @Test
    fun boxNullable() {
        runnerWithDefaultOptions(::boxNullable)
    }

    @Test
    fun boxStruct() {
        runnerWithDefaultOptions(::boxStruct)
    }

}
