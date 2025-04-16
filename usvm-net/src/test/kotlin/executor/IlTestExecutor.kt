package executor

import com.google.protobuf.Message
import com.jetbrains.rd.framework.util.NetUtils
import common.IlTestStateResolver
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.machine.IlContext
import org.usvm.machine.interpreter.IlMethodResult
import org.usvm.machine.logger
import org.usvm.machine.state.IlState
import org.usvm.memory.UReadOnlyMemory
import org.usvm.model.UModelBase
import testrunner.expressions.*
import java.io.Closeable
import kotlin.math.log

class IlTestExecutor : Closeable {
    private val port = NetUtils.findFreePort(0)
    private val dotnetProc : Process = RunnerProcessBuilder.build(port).start()
    private val concreteRunner = ConcreteTestRunner(dotnetProc, port)

    fun execute(states: List<IlState>, method: IlMethod) : TestExpressions.ExecutionResult {
        val tests = states.map { state ->
            val model = state.models.first()
            val memory = state.memory
            val scope = MemoryScope(state.ctx, method, state.methodResult, model, memory)
            scope.createTest()
        }
        val batch = ilTestBatch {
            this.tests.addAll(tests)
        }

        return concreteRunner.run(batch)
    }

    private class MemoryScope(
        ctx: IlContext,
        method: IlMethod,
        result: IlMethodResult,
        model: UModelBase<IlType>,
        stateMemory: UReadOnlyMemory<IlType>
    ) : IlTestStateResolver<Message>(ctx, method, result, model, stateMemory) {
        override val decoderApi: IlTestExecutorDecoderApi = IlTestExecutorDecoderApi(ctx)

        fun createTest(): TestExpressions.IlTest {
            val args = resolveArgs()
            val arrange = decoderApi.arrangeStmts().map { com.google.protobuf.Any.pack(it) }
            val methodCall = decoderApi.callMethod(method, args)
            val resultAsMessage = when (result) {
                is IlMethodResult.Success -> resolve(result.result, method.returnType)
                is IlMethodResult.Exception -> resolve(result.exception, result.type)
                else -> TODO()
            }
            val test = ilTest {
                arrangeStmts.addAll(arrange)
                call = com.google.protobuf.Any.pack(methodCall)
                expectedResult = com.google.protobuf.Any.pack(resultAsMessage)
                raisesCriticalError = false
            }
            return test
        }
    }

    override fun close() {
        dotnetProc.destroy()
    }
}
