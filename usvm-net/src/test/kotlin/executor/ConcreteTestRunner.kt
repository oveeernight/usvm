package executor

import IlMethodTestRunner
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.usvm.machine.logger
import testrunner.expressions.ConcreteExecutorGrpcKt
import testrunner.expressions.TestExpressions
import java.io.Closeable
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ConcreteTestRunner(val proc: Process) {

    fun run(testBatch: TestExpressions.IlTestBatch) : TestExpressions.ExecutionResult {
        val port = 8980
        val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
        val stub = ConcreteExecutorGrpcKt.ConcreteExecutorCoroutineStub(channel)

        val result = runBlocking {
            delay(1000)
            stub.execute(testBatch)
        }

        val isSuccess = result.resultCase == TestExpressions.ExecutionResult.ResultCase.SUCCESS
        logger.error { proc.errorStream.bufferedReader().readText() }
        require(isSuccess) {
            val failReason = result.fail.reason
            "Some executions failed:\n$failReason"
        }
        return result
    }

}
