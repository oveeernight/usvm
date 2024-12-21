package executor

import IlMethodTestRunner
import org.usvm.machine.logger
import testrunner.expressions.TestExpressions
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.log

class ConcreteTestRunner(val timeoutSec: Int) {
    fun run(test: TestExpressions.IlTest) {
        val samplesPath = IlMethodTestRunner.samplesAsm
        val executorPath = IlMethodTestRunner.executorPath
        val executorDir = File(executorPath)
        val file = File.createTempFile("serialized-test", ".txt")
        file.writeBytes(test.toByteArray())
        val filePath = file.absolutePath;
        val proc = ProcessBuilder().command("./Application", "--src", samplesPath, "--test", filePath)
            .directory(executorDir)
            .start()

        proc.waitFor(timeoutSec.toLong(), TimeUnit.SECONDS)

        val output = proc.inputStream.bufferedReader().readText()
        val error = proc.errorStream.bufferedReader().readText()
        logger.info {"got an answer from dotnet"}
        logger.info { "output: $output" }
        logger.info { "error: $error" }
        logger.info { "exit value: ${proc.exitValue()}" }
    }

//    val f = methodCall.argsList.add(int)
}
