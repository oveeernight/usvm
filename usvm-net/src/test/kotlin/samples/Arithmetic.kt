package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Arithmetic : IlMethodTestRunner() {
    @Test
    fun addInts() {
        runner(::addInts, options)
    }

    @Test
    fun subInts() {
        runner(::subInts, options)
    }

    @Test
    fun multiplyInts() {
        runner(::multiplyInts, options)
    }

    @Test
    fun divideInts() {
        runner(::divideInts, options)
    }

    @Test
    fun gt() {
        runner(::gt, options)
    }

    @Test
    fun ge() {
        runner(::ge, options)
    }

    @Test
    fun lt() {
        runner(::lt, options)
    }

    @Test
    fun le() {
        runner(::le, options)
    }


    @Test
    fun shl() {
        runner(::shl, options)
    }


    @Test
    fun multiplicationOfFloatsIsNotAssociative() {
        runnerWithDefaultOptions(::multiplicationOfFloatsIsNotAssociative)
    }

    @Test
    fun multiplicationOfDoublesIsNotAssociative() {
        runnerWithDefaultOptions(::multiplicationOfDoublesIsNotAssociative)
    }

    @Test
    fun divideWithoutOverflow() {
        runnerWithDefaultOptions(::divideWithoutOverflow)
    }

    @Test
    fun divideFloatOnZero() {
        runnerWithDefaultOptions(::divideFloatOnZero)
    }

    @Test
    fun divideDoubleOnZero() {
        runnerWithDefaultOptions(::divideDoubleOnZero)
    }

    @Test
    fun divideOnZero1() {
        runnerWithDefaultOptions(::divideOnZero1)
    }

    @Test
    fun divideOnZero2() {
        runnerWithDefaultOptions(::divideOnZero2)
    }

    @Test
    fun addFloats() {
        runnerWithDefaultOptions(::addFloats)
    }

    @Test
    fun addDoubles() {
        runnerWithDefaultOptions(::addDoubles)
    }

    @Test
    fun addChecked() {
        runnerWithDefaultOptions(::addChecked)
    }

    @Test
    fun addUnsigned() {
        runnerWithDefaultOptions(::addUnsigned)
    }

    @Test
    fun addOvfUn() {
        runnerWithDefaultOptions(::addOvfUn)
    }

    @Test
    fun mulFloats() {
        runnerWithDefaultOptions(::mulFloats)
    }

    @Test
    fun mulDoubles() {
        runnerWithDefaultOptions(::mulDoubles)
    }

    @Test
    fun mulOvfUn() {
        runnerWithDefaultOptions(::mulOvfUn)
    }

    @Test
    fun mulOvf() {
        runnerWithDefaultOptions(::mulOvf)
    }

    @Test
    fun mulOvf64() {
        runnerWithDefaultOptions(::mulOvf64)
    }

    @Test
    fun mulOvfU64() {
        runnerWithDefaultOptions(::mulOvfU64)
    }

    @Test
    fun subFloats() {
        runnerWithDefaultOptions(::subFloats)
    }

    @Test
    fun subDoubles() {
        runnerWithDefaultOptions(::subDoubles)
    }

    @Test
    fun subOvf() {
        runnerWithDefaultOptions(::subOvf)
    }

    @Test
    fun subOvfUn() {
        runnerWithDefaultOptions(::subOvfUn)
    }

    @Test
    fun addSbyteShort() {
        runnerWithDefaultOptions(::addSbyteShort)
    }

    @Test
    fun remFloats() {
        runnerWithDefaultOptions(::remFloats)
    }

    @Test
    fun remConcreteFloats() {
        runnerWithDefaultOptions(::remConcreteFloats)
    }

    @Test
    fun remDoubles() {
        runnerWithDefaultOptions(::remDoubles)
    }

    @Test
    fun remConcreteDoubles() {
        runnerWithDefaultOptions(::remConcreteDoubles)
    }

    @Test
    fun remInts() {
        runnerWithDefaultOptions(::remInts)
    }

    @Test
    fun remIntsDivideOnZero() {
        runnerWithDefaultOptions(::remIntsDivideOnZero)
    }

    @Test
    fun remUnInts() {
        runnerWithDefaultOptions(::remUnInts)
    }

    @Test
    fun remUnIntsDivideOnZero() {
        runnerWithDefaultOptions(::remUnIntsDivideOnZero)
    }

    @Test
    fun arithmeticsMethod1() {
        runnerWithDefaultOptions(::arithmeticsMethod1)
    }

    @Test
    fun arithmeticsMethod2() {
        runnerWithDefaultOptions(::arithmeticsMethod2)
    }

    @Test
    fun arithmeticsMethod3() {
        runnerWithDefaultOptions(::arithmeticsMethod3)
    }

    @Test
    fun arithmeticsMethod4() {
        runnerWithDefaultOptions(::arithmeticsMethod4)
    }

    // fix tac
    @Test
    fun incrementsWorkCorrect() {
        runnerWithDefaultOptions(::incrementsWorkCorrect)
    }

    @Test
    fun bigSum() {
        runnerWithDefaultOptions(::bigSum)
    }

    @Test
    fun smallBigSum() {
        runnerWithDefaultOptions(::smallBigSum)
    }

    @Ignore("")
    @Test
    fun bigSumCycle() {
        runnerWithDefaultOptions(::bigSumCycle)
    }

    @Test
    fun decreasing() {
        runnerWithDefaultOptions(::decreasing)
    }

    @Test
    fun checkedUnchecked() {
        runnerWithDefaultOptions(::checkedUnchecked)
    }

    @Test
    fun checkOverflow1() {
        runnerWithDefaultOptions(::checkOverflow1)
    }

    @Test
    fun checkOverflow2() {
        runnerWithDefaultOptions(::checkOverflow2)
    }

    @Test
    fun sumOfIntAndUint() {
        runnerWithDefaultOptions(::sumOfIntAndUint)
    }

    @Test
    fun sumOfIntAndShort() {
        runnerWithDefaultOptions(::sumOfIntAndShort)
    }

    @Test
    fun checkDivideByZeroException0() {
        runnerWithDefaultOptions(::checkDivideByZeroException0)
    }

    @Test
    fun checkOrder() {
        runnerWithDefaultOptions(::checkOrder)
    }

    @Test
    fun shiftLeftOnZero() {
        runnerWithDefaultOptions(::shiftLeftOnZero)
    }

    @Test
    fun zeroShift() {
        runnerWithDefaultOptions(::zeroShift)
    }

    @Test
    fun defaultShift() {
        runnerWithDefaultOptions(::defaultShift)
    }

    @Test
    fun sumShifts() {
        runnerWithDefaultOptions(::sumShifts)
    }

    @Test
    fun shiftSum() {
        runnerWithDefaultOptions(::shiftSum)
    }

    @Test
    fun multiplyOnShift1() {
        runnerWithDefaultOptions(::multiplyOnShift1)
    }

    @Test
    fun multiplyOnShift2() {
        runnerWithDefaultOptions(::multiplyOnShift2)
    }

    @Test
    fun shiftMultiplication() {
        runnerWithDefaultOptions(::shiftMultiplication)
    }

    @Test
    fun shiftDivision1() {
        runnerWithDefaultOptions(::shiftDivision1)
    }

    @Test
    fun shiftDivision2() {
        runnerWithDefaultOptions(::shiftDivision2)
    }

    @Test
    fun shiftDivision3() {
        runnerWithDefaultOptions(::shiftDivision3)
    }

    @Test
    fun shiftDivision4() {
        runnerWithDefaultOptions(::shiftDivision4)
    }

    @Test
    fun shrUn() {
        runnerWithDefaultOptions(::shrUn)
    }

    @Test
    fun shr() {
        runnerWithDefaultOptions(::shr)
    }

    @Test
    fun shrTest() {
        runnerWithDefaultOptions(::shrTest)
    }

    @Test
    fun shiftSumOfShifts1() {
        runnerWithDefaultOptions(::shiftSumOfShifts1)
    }

    @Test
    fun shiftSumOfShifts2() {
        runnerWithDefaultOptions(::shiftSumOfShifts2)
    }

    @Test
    fun concreteShift() {
        runnerWithDefaultOptions(::concreteShift)
    }

    @Test
    fun multiplyShifts1() {
        runnerWithDefaultOptions(::multiplyShifts1)
    }

    @Test
    fun multiplyShifts2() {
        runnerWithDefaultOptions(::multiplyShifts2)
    }

    @Test
    fun shiftWithDivAndMul() {
        runnerWithDefaultOptions(::shiftWithDivAndMul)
    }

    @Test
    fun doubleShiftRight() {
        runnerWithDefaultOptions(::doubleShiftRight)
    }

    @Test
    fun encodeDoubleTest() {
        runnerWithDefaultOptions(::encodeDoubleTest)
    }

    @Test
    fun encodeDoubleTest1() {
        runnerWithDefaultOptions(::encodeDoubleTest1)
    }

    @Test
    fun encodeFloatTest() {
        runnerWithDefaultOptions(::encodeFloatTest)
    }

    @Test
    fun encodeFloatTest1() {
        runnerWithDefaultOptions(::encodeFloatTest1)
    }

    @Test
    fun compareDoubleAndFloatTest1() {
        runnerWithDefaultOptions(::compareDoubleAndFloatTest1)
    }

    @Test
    fun compareDoubleAndFloatTest2() {
        runnerWithDefaultOptions(::compareDoubleAndFloatTest2)
    }

    @Test
    fun compareDoubleAndFloatTest3() {
        runnerWithDefaultOptions(::compareDoubleAndFloatTest3)
    }

    @Test
    fun compareDoubleAndFloatTest4() {
        runnerWithDefaultOptions(::compareDoubleAndFloatTest4)
    }

    @Test
    fun compareDoublesTest1() {
        runnerWithDefaultOptions(::compareDoublesTest1)
    }

    @Test
    fun compareDoublesTest2() {
        runnerWithDefaultOptions(::compareDoublesTest2)
    }

    @Test
    fun compareDoublesTest3() {
        runnerWithDefaultOptions(::compareDoublesTest3)
    }

    @Test
    fun compareDoublesTest4() {
        runnerWithDefaultOptions(::compareDoublesTest4)
    }

    @Test
    fun compareFloatsTest1() {
        runnerWithDefaultOptions(::compareFloatsTest1)
    }

    @Test
    fun compareFloatsTest2() {
        runnerWithDefaultOptions(::compareFloatsTest2)
    }

    @Test
    fun compareFloatsTest3() {
        runnerWithDefaultOptions(::compareFloatsTest3)
    }

    @Test
    fun compareFloatsTest4() {
        runnerWithDefaultOptions(::compareFloatsTest4)
    }

    @Test
    fun castRealToIntegral() {
        runnerWithDefaultOptions(::castRealToIntegral)
    }

    @Test
    fun castRealToIntegral1() {
        runnerWithDefaultOptions(::castRealToIntegral1)
    }

    @Test
    fun castRealToIntegral2() {
        runnerWithDefaultOptions(::castRealToIntegral2)
    }

    @Test
    fun castRealToIntegral3() {
        runnerWithDefaultOptions(::castRealToIntegral3)
    }

    @Test
    fun castRealToIntegral4() {
        runnerWithDefaultOptions(::castRealToIntegral4)
    }

    @Test
    fun castRealToIntegral5() {
        runnerWithDefaultOptions(::castRealToIntegral5)
    }

    @Test
    fun castIntegralToReal() {
        runnerWithDefaultOptions(::castIntegralToReal)
    }

    @Test
    fun castIntegralToReal1() {
        runnerWithDefaultOptions(::castIntegralToReal1)
    }

    @Test
    fun castIntegralToReal2() {
        runnerWithDefaultOptions(::castIntegralToReal2)
    }

    @Test
    fun castIntegralToReal3() {
        runnerWithDefaultOptions(::castIntegralToReal3)
    }

    @Test
    fun castIntegralToReal4() {
        runnerWithDefaultOptions(::castIntegralToReal4)
    }

    @Test
    fun castIntegralToReal5() {
        runnerWithDefaultOptions(::castIntegralToReal5)
    }
}
