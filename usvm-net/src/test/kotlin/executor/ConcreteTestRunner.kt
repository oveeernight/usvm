package executor

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import testrunner.expressions.ConcreteExecutorGrpcKt
import testrunner.expressions.TestExpressions
import java.io.Closeable

object ConcreteTestRunnerContainer {
    lateinit var runner: ConcreteTestRunner
    val isInitialized: Boolean
        get() = ::runner.isInitialized
    fun init(port: Int) {
        runner = ConcreteTestRunner(port)
    }
}

class ConcreteTestRunner(private val serverPort: Int): Closeable {
    private val dotnetProc : Process = RunnerProcessBuilder.build(serverPort).start()

    fun run(testBatch: TestExpressions.IlTestBatch) : TestExpressions.ExecutionResult {
        val channel = ManagedChannelBuilder.forAddress("localhost", serverPort).usePlaintext().enableRetry().build()
        val stub = ConcreteExecutorGrpcKt.ConcreteExecutorCoroutineStub(channel)

        val result = runBlocking {
            delay(500)
            stub.execute(testBatch)
        }
        channel.shutdown()

        val isSuccess = result.resultCase == TestExpressions.ExecutionResult.ResultCase.SUCCESS
        require(isSuccess) {
            val failReason = result.fail.reason
            "Some executions failed:\n$failReason"
        }
        return result
    }

    override fun close() {
        dotnetProc.destroy()
    }

}
