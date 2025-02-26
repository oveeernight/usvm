package samples

import IlMethodTestRunner
import kotlin.test.Test

class Calls : IlMethodTestRunner() {
    @Test
    fun mulOrAdd() {
        runner(::mulOrAdd, options)
    }
}
