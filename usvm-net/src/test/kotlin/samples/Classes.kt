package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Classes : IlMethodTestRunner() {
    @Ignore("Wait for type solver")
    @Test
    fun concreteObjectDefaultValue() {
        runner(::concreteObjectDefaultValue, options)
    }
}
