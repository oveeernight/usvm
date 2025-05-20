package samples

import IlMethodTestRunner
import org.junit.jupiter.api.TestInstance
import kotlin.test.Ignore
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Classes : IlMethodTestRunner() {
    @Test
    fun symbolicClassSet() {
        runnerWithDefaultOptions(::symbolicClassSet)
    }

    @Test
    fun symbolicCyclicListNode() {
        runnerWithDefaultOptions(::symbolicCyclicListNode)
    }
    @Test
    fun concreteObjectDefaultValue() {
        runnerWithDefaultOptions(::concreteObjectDefaultValue)
    }

    @Test
    fun fieldsInequalityImpliesObjectsInequality() {
        runnerWithDefaultOptions(::fieldsInequalityImpliesObjectsInequality)
    }

    @Test
    fun staticCtorTest1() {
        runnerWithDefaultOptions(::staticCtorTest1)
    }

    @Test
    fun staticCtorTest2() {
        runnerWithDefaultOptions(::staticCtorTest2)
    }

    @Test
    fun staticCtorTest3() {
        runnerWithDefaultOptions(::staticCtorTest3)
    }

    @Test
    fun recObject() {
        runnerWithDefaultOptions(::recObject)
    }
}
