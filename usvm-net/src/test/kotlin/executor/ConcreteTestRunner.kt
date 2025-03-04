package executor

import IlMethodTestRunner
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.usvm.machine.logger
import testrunner.expressions.ConcreteExecutorGrpcKt
import testrunner.expressions.TestExpressions

class ConcreteTestRunner(val proc: Process) {

    fun run(testBatch: TestExpressions.IlTestBatch) : TestExpressions.ExecutionResult {
        val port = 8980
        val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().enableRetry().build()
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
