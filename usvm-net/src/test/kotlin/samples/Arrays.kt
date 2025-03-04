package samples

import IlMethodTestRunner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Ignore
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Arrays : IlMethodTestRunner() {

    @AfterAll
    fun tearDown() {
        executor.close()
    }
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
