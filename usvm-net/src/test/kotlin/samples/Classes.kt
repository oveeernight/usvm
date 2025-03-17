package samples

import IlMethodTestRunner
import org.junit.jupiter.api.TestInstance
import kotlin.test.Ignore
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Classes : IlMethodTestRunner() {
    @Test
    fun symbolicClassSet() {
        runner(::symbolicClassSet, options)
    }

    @Test
    fun symbolicCyclicListNode() {
        runner(::symbolicCyclicListNode, options)
    }
    @Ignore("Wait for type solver")
    @Test
    fun concreteObjectDefaultValue() {
        runner(::concreteObjectDefaultValue, options)
    }
}
