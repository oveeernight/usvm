package samples

import IlMethodTestRunner
import kotlin.test.Test

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
    fun structsArrayConcreteWrite() {
        runner(::structsArrayConcreteWrite, options)
    }

    @Test
    fun structsArraySymbolicReading() {
        runner(::structsArraySymbolicReading, options)
    }

    @Test
    fun symbolicStructField() {
        runner(::symbolicStructField, options)
    }

    @Test
    fun structsAliasing() {
        runner(::structsAliasing, options)
    }

    @Test
    fun structsAsClassFieldsAliasing() {
        runner(::structsAsClassFieldsAliasing, options)
    }
}
