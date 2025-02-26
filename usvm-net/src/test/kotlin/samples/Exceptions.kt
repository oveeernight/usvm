package samples

import IlMethodTestRunner
import kotlin.test.Ignore
import kotlin.test.Test

class Exceptions : IlMethodTestRunner() {
    @Ignore("Wait for eh statements")
    @Test
    fun indexOutOfBounds() {
        runner(::indexOutOfBounds, options)
    }

}
