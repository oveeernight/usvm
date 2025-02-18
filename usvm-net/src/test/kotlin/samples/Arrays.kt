package samples

import IlMethodTestRunner
import kotlin.test.Test

class Arrays : IlMethodTestRunner() {

    @Test
    fun arrayStore() {
        runMethod(method = ::arrayStore)
    }
}
