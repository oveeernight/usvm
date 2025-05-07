package samples

import IlMethodTestRunner
import kotlin.test.Test

class Generics : IlMethodTestRunner() {
    @Test
    fun genericClassValue() {
        runnerWithDefaultOptions(::genericClassValue)
    }

}
