package executor

import java.io.File

class TestRunner {
    val executorPath = File("")
    fun run(test: IlTest) {
        val t = ProcessBuilder().command("./TestExecutor").directory(executorPath).start()
    }
}
