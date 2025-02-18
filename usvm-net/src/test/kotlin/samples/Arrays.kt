package samples

import IlMethodTestRunner
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
}
