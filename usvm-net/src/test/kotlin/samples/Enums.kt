package samples

import IlMethodTestRunner
import kotlin.test.Test

class Enums: IlMethodTestRunner() {
    @Test
    fun enumToInt() {
        runnerWithDefaultOptions(::enumToInt)
    }
}
