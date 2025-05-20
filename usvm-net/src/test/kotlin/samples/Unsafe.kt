package samples

import IlMethodTestRunner
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test

class Unsafe : IlMethodTestRunner() {
    @Test
    fun byrefVar() {
        runnerWithDefaultOptions(::byrefVar)
    }

    @Test
    fun argumentWrite() {
        runnerWithDefaultOptions(::argumentWrite)
    }

    @Test
    fun concreteStackUnsafe() {
        runnerWithDefaultOptions(::concreteStackUnsafe)
    }

    @Test
    fun symbolicStackUnsafe1() {
        runnerWithDefaultOptions(::symbolicStackUnsafe1)
    }

    @Test
    fun symbolicStackUnsafe2() {
        runnerWithDefaultOptions(::symbolicStackUnsafe2)
    }

    @Ignore("")
    @Test
    fun detachedPtr() {
        runnerWithDefaultOptions(::detachedPtr)
    }

    @Test
    fun concreteArrayWrite1() {
        runnerWithDefaultOptions(::concreteArrayWrite1)
    }

    @Test
    fun concreteArrayWrite2() {
        runnerWithDefaultOptions(::concreteArrayWrite2)
    }

    @Test
    fun concreteArrayWrite3() {
        runnerWithDefaultOptions(::concreteArrayWrite3)
    }

    @Test
    fun symbolicArrayWrite1() {
        runnerWithDefaultOptions(::symbolicArrayWrite1)
    }

    @Test
    fun symbolicArrayWrite2() {
        runnerWithDefaultOptions(::symbolicArrayWrite2)
    }

    @Test
    fun symbolicArrayWrite3() {
        runnerWithDefaultOptions(::symbolicArrayWrite3)
    }

    @Test
    fun symbolicArrayWriteAffectingTwoElements() {
        runnerWithDefaultOptions(::symbolicArrayWriteAffectingTwoElements)
    }

    @Test
    fun symbolicArrayWriteAffectingThreeElements() {
        runnerWithDefaultOptions(::symbolicArrayWriteAffectingThreeElements)
    }

    @Test
    fun symbolicArrayWriteOfSameBytes() {
        runnerWithDefaultOptions(::symbolicArrayWriteOfSameBytes)
    }

    @Test
    fun symbolicArrayRead1() {
        runnerWithDefaultOptions(::symbolicArrayRead1)
    }

    @Test
    fun symbolicArrayRead2() {
        runnerWithDefaultOptions(::symbolicArrayRead2)
    }


    @Test
    fun refField() {
        runnerWithDefaultOptions(::refField)
    }

    @Test
    fun concreteStructWrite() {
        runnerWithDefaultOptions(::concreteStructWrite)
    }

    @Test
    fun symbolicWriteInStructsArray1() {
        runnerWithDefaultOptions(::symbolicWriteInStructsArray1)
    }

    @Test
    fun symbolicWriteInStructsArray2() {
        runnerWithDefaultOptions(::symbolicWriteInStructsArray2)
    }

    @Test
    fun symbolicWriteInStructsArray3() {
        runnerWithDefaultOptions(::symbolicWriteInStructsArray3)
    }

    @Test
    fun symbolicReadInStructsArray1() {
        runnerWithDefaultOptions(::symbolicReadInStructsArray1)
    }

    @Test
    fun symbolicReadInStructsArray2() {
        runnerWithDefaultOptions(::symbolicReadInStructsArray2)
    }

    @Test
    fun classSymbolicUnsafeRead1() {
        runnerWithDefaultOptions(::classSymbolicUnsafeRead1)
    }

    @Test
    fun classSymbolicUnsafeRead2() {
        runnerWithDefaultOptions(::classSymbolicUnsafeRead2)
    }

    @Test
    fun classSymbolicReadZeroBetweenFields() {
        runnerWithDefaultOptions(::classSymbolicReadZeroBetweenFields)
    }

    @Test
    fun classWriteSafeOverlappingFields() {
        runnerWithDefaultOptions(::classWriteSafeOverlappingFields)
    }




}
