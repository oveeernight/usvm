package executor

import java.io.File
import java.util.concurrent.TimeUnit

@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
class ConcreteTestRunner(val asmPath: String, val timeoutSec: Int) {
    val executorPath = File("")
    fun run(test: IlTest) {
        val proc = ProcessBuilder().command("./TestExecutor", asmPath)
            .directory(executorPath)
            .start()

        proc.waitFor(timeoutSec.toLong(), TimeUnit.SECONDS)

        val response = proc.inputStream.bufferedReader().readText()
    }

//    val f = methodCall.argsList.add(int)
}
