package executor

import java.io.File

class TestRunner {
    val executorPath = File("")
    fun run(test: IlTest) {
        println(test)
        ProcessBuilder().command("./TestExecutor").directory(executorPath).start()
    }
}
