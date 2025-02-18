package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Arrays : IlMethodTestRunner() {

    @Test
    fun arrayStore() {
        runMethod(method = ::arrayStore)
    }

    @Test
    fun arraySimpleBranch() {
        runMethod(method = ::arraySimpleBranch)
    }

    @Ignore("unsafe")
    @Test
    fun stringIndex() {
        runMethod(method = ::stringIndex)
    }
}
