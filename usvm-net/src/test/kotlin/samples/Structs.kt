package samples

import IlMethodTestRunner
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Structs : IlMethodTestRunner() {
    @Test
    fun writeStructConcrete() {
        runner(::writeStructConcrete, options)
    }

    @Test
    fun structImmutabilityCheck() {
        runner(::structImmutabilityCheck, options)
    }

    @Test
    fun structMutabilityCheck() {
        runner(::structMutabilityCheck, options)
    }

    @Test
    fun concreteArrayOfStructs() {
        runner(::concreteArrayOfStructs, options)
    }
}
