package executor

import IlMethodTestRunner
import org.usvm.machine.logger
import testrunner.expressions.TestExpressions
import java.io.File
import java.util.concurrent.TimeUnit

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

        val response = proc.inputStream.bufferedReader().readText()
        logger.info {"got an answer from dotnet"}
        logger.info { response }
    }

//    val f = methodCall.argsList.add(int)
}
