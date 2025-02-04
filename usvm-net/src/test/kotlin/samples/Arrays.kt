package samples

import IlMethodTestRunner
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

    @Ignore("unsafe")
    @Test
    fun stringIndex() {
        runner(::stringIndex, options)
    }
}
