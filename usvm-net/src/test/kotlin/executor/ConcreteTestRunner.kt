package executor

import IlMethodTestRunner
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import org.usvm.machine.logger
import testrunner.expressions.ConcreteExecutorGrpcKt
import testrunner.expressions.TestExpressions
import java.io.File
import java.util.concurrent.TimeUnit

class ConcreteTestRunner() {
    fun run(test: TestExpressions.IlTest) : TestExpressions.ExecutionResult {
        val samplesPath = IlMethodTestRunner.samplesAsmPath
        val executorPath = IlMethodTestRunner.executorPath
        val executorDir = File(executorPath)
        val proc = ProcessBuilder().command("./Application", "--src", samplesPath)
            .directory(executorDir)
            .start()

        val port = 8980
        val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
        val stub = ConcreteExecutorGrpcKt.ConcreteExecutorCoroutineStub(channel)

        val result = runBlocking {
            stub.execute(test)
        }

        val isSuccess = result.resultCase == TestExpressions.ExecutionResult.ResultCase.SUCCESS
        require(isSuccess) {
            val failReason = result.fail.reason
            "Some executions failed:\n$failReason"
        }

        val error = proc.errorStream.bufferedReader().readText()
        if (proc.exitValue() != 0)
            logger.error { "Internal error occured while concrete executing: $error" }
        return result
    }

}
