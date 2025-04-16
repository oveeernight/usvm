package executor

import IlMethodTestRunner
import com.jetbrains.rd.framework.util.NetUtils
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.usvm.machine.logger
import testrunner.expressions.ConcreteExecutorGrpcKt
import testrunner.expressions.TestExpressions

class ConcreteTestRunner(private val proc: Process, private val serverPort: Int) {

    fun run(testBatch: TestExpressions.IlTestBatch) : TestExpressions.ExecutionResult {
        val channel = ManagedChannelBuilder.forAddress("localhost", serverPort).usePlaintext().enableRetry().build()
        val stub = ConcreteExecutorGrpcKt.ConcreteExecutorCoroutineStub(channel)

        val result = runBlocking {
            delay(500)
            stub.execute(testBatch)
        }

        val isSuccess = result.resultCase == TestExpressions.ExecutionResult.ResultCase.SUCCESS
        require(isSuccess) {
            proc.destroy()
            val failReason = result.fail.reason
            "Some executions failed:\n$failReason"
        }
        return result
    }

}
