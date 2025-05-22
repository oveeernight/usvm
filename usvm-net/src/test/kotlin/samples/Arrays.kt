package samples

import IlMethodTestRunner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Ignore
import kotlin.test.Test

class Arrays : IlMethodTestRunner() {
    @Test
    fun arrayStore() {
        runner(::arrayStore, options)
    }

    @Test
    fun arraySimpleBranch() {
        runner(::arraySimpleBranch, options)
    }

    @Test
    fun concreteArraySymbolicIndex() {
        runner(::concreteArraySymbolicIndex, options)
    }

    @Test
    fun classesArray() {
        runnerWithDefaultOptions(::classesArray)
    }

    @Test
    fun copyAndBranch() {
        runnerWithDefaultOptions(::copyAndBranch)
    }

    @Test
    fun copyConcreteToConcreteArray() {
        runnerWithDefaultOptions(::copyConcreteToConcreteArray)
    }

    @Ignore
    @Test
    fun copyConcreteToSymbolicArray() {
        runnerWithDefaultOptions(::copyConcreteToSymbolicArray)
    }

    @Test
    fun copyAndThenWrite() {
        runnerWithDefaultOptions(::copyAndThenWrite)
    }

    @Ignore
    @Test
    fun doubleWriteAfterCopy() {
        runnerWithDefaultOptions(::doubleWriteAfterCopy)
    }

    @Ignore
    @Test
    fun doubleWriteAfterCopy1() {
        runnerWithDefaultOptions(::doubleWriteAfterCopy1)
    }

    @Test
    fun copySymbolicIndicesToConcreteArray() {
        runnerWithDefaultOptions(::copySymbolicIndicesToConcreteArray)
    }

    @Test
    fun copySymbolicIndicesToConcreteArray1() {
        runnerWithDefaultOptions(::copySymbolicIndicesToConcreteArray1)
    }

    @Test
    fun copySymbolicIndicesToConcreteArray2() {
        runnerWithDefaultOptions(::copySymbolicIndicesToConcreteArray2)
    }

    @Ignore
    @Test
    fun testSolvingCopy() {
        runnerWithDefaultOptions(::testSolvingCopy)
    }

    @Test
    fun testSolvingCopy1() {
        runnerWithDefaultOptions(::testSolvingCopy1)
    }

    @Test
    fun testSolvingCopy2() {
        runnerWithDefaultOptions(::testSolvingCopy2)
    }

    @Test
    fun testSolvingCopy3() {
        runnerWithDefaultOptions(::testSolvingCopy3)
    }

    @Test
    fun testSolvingCopy4() {
        runnerWithDefaultOptions(::testSolvingCopy4)
    }

    @Ignore
    @Test
    fun testSolvingCopy5() {
        runnerWithDefaultOptions(::testSolvingCopy5)
    }

    @Ignore
    @Test
    fun testSolvingCopy6() {
        runnerWithDefaultOptions(::testSolvingCopy6)
    }

    @Test
    fun testSolvingCopy7() {
        runnerWithDefaultOptions(::testSolvingCopy7)
    }

    @Ignore
    @Test
    fun testSolvingCopy8() {
        runnerWithDefaultOptions(::testSolvingCopy8)
    }

    @Test
    fun testSolvingCopy9() {
        runnerWithDefaultOptions(::testSolvingCopy9)
    }

    @Ignore
    @Test
    fun testSolvingCopy10() {
        runnerWithDefaultOptions(::testSolvingCopy10)
    }

    @Ignore
    @Test
    fun testSolvingCopy11() {
        runnerWithDefaultOptions(::testSolvingCopy11)
    }

    @Test
    fun testOverlappingCopy() {
        runnerWithDefaultOptions(::testOverlappingCopy)
    }

    @Test
    fun testOverlappingCopy1() {
        runnerWithDefaultOptions(::testOverlappingCopy1)
    }

    @Test
    fun testSolvingCopyOverwrittenValueUnreachable1() {
        runnerWithDefaultOptions(::testSolvingCopyOverwrittenValueUnreachable1)
    }

    @Test
    fun testSolvingCopyOverwrittenValueUnreachable2() {
        runnerWithDefaultOptions(::testSolvingCopyOverwrittenValueUnreachable2)
    }

    @Test
    fun arrayAliasWrite() {
        runnerWithDefaultOptions(::arrayAliasWrite)
    }

    @Test
    fun symbolicWriteAfterConcreteWrite() {
        runnerWithDefaultOptions(::symbolicWriteAfterConcreteWrite)
    }

    @Test
    fun symbolicWriteAfterConcreteWrite2() {
        runnerWithDefaultOptions(::symbolicWriteAfterConcreteWrite2)
    }

    @Test
    fun solverTestArrayKey() {
        runnerWithDefaultOptions(::solverTestArrayKey)
    }

    @Test
    fun retOneDArray2() {
        runnerWithDefaultOptions(::retOneDArray2)
    }

    @Test
    fun lastRecordReachability() {
        runnerWithDefaultOptions(::lastRecordReachability)
    }

    @Test
    fun arrayElementsAreReferences() {
        runnerWithDefaultOptions(::arrayElementsAreReferences)
    }

    @Test
    fun arraySymbolicUpdate() {
        runnerWithDefaultOptions(::arraySymbolicUpdate)
    }

    @Test
    fun arraySymbolicUpdate2() {
        runnerWithDefaultOptions(::arraySymbolicUpdate2)
    }

    @Test
    fun arraySymbolicUpdate3() {
        runnerWithDefaultOptions(::arraySymbolicUpdate3)
    }

    @Test
    fun typeSolverCheck() {
        runnerWithDefaultOptions(::typeSolverCheck)
    }

    @Test
    fun iteKeyWrite() {
        runnerWithDefaultOptions(::iteKeyWrite)
    }

    @Test
    fun arrayExceptionsOrder() {
        runnerWithDefaultOptions(::arrayExceptionsOrder)
    }





//
//    @Ignore("unsafe")
//    @Test
//    fun stringIndex() {
//        runner(::stringIndex, options)
//    }
}
