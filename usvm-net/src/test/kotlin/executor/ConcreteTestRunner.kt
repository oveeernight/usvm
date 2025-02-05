package executor

import IlMethodTestRunner
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import org.usvm.machine.logger
import testrunner.expressions.ConcreteExecutorGrpcKt
import testrunner.expressions.TestExpressions
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ConcreteTestRunner() {
    fun run(test: TestExpressions.IlTest) : TestExpressions.ExecutionResult {
        val executorPath = IlMethodTestRunner.executorPath
        val executorDir = File(executorPath)
        val samplesAsm = IlMethodTestRunner.samplesAsmPath;

        val proc = ProcessBuilder().command("./TestExecutor.Application", "--asm", samplesAsm)
            .directory(executorDir)
            .start()
        val port = 8980
        val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
        val stub = ConcreteExecutorGrpcKt.ConcreteExecutorCoroutineStub(channel)
        val result = runBlocking {
            stub.execute(test)
        }
        shutdownServer()
        val error = proc.errorStream.bufferedReader().readText()
        logger.error { "Internal error occured while concrete executing: $error" }

        val isSuccess = result.resultCase == TestExpressions.ExecutionResult.ResultCase.SUCCESS
        require(isSuccess) {
            val failReason = result.fail.reason
            "Some executions failed:\n$failReason"
        }
//
        return result
    }

    private fun shutdownServer() {
        val url = URL("http://localhost:8980/shutdown")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
    }

}
