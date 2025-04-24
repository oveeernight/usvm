package samples

import IlMethodTestRunner
import kotlin.test.Test

class Structs : IlMethodTestRunner() {
    @Test
    fun sourceStructUnaffectedAfterWriteOnCopy() {
        runnerWithDefaultOptions(::sourceStructUnaffectedAfterWriteOnCopy)
    }
    @Test
    fun writeStructConcrete() {
        runnerWithDefaultOptions(::writeStructConcrete)
    }

    @Test
    fun structImmutabilityCheck() {
        runnerWithDefaultOptions(::structImmutabilityCheck)
    }

    @Test
    fun structMutabilityCheck() {
        runnerWithDefaultOptions(::structMutabilityCheck)
    }

    @Test
    fun structsArrayConcreteWrite() {
        runnerWithDefaultOptions(::structsArrayConcreteWrite)
    }

    @Test
    fun structsArraySymbolicReading() {
        runnerWithDefaultOptions(::structsArraySymbolicReading)
    }

    @Test
    fun symbolicStructField() {
        runnerWithDefaultOptions(::symbolicStructField)
    }

    @Test
    fun structsAliasing() {
        runnerWithDefaultOptions(::structsAliasing)
    }

    @Test
    fun structsAsClassFieldsAliasing() {
        runnerWithDefaultOptions(::structsAsClassFieldsAliasing)
    }
}
