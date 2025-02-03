package executor

import com.google.protobuf.Message
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

class IlTestConcreteExecutor(private val state: IlState, private val method: IlMethod) {
    private val concreteRunner = ConcreteTestRunner(timeoutSec = 5)
    fun execute() {
        val model = state.models.first()
        val memory = state.memory
        val scope = MemoryScope(state.ctx, method, state.methodResult, model, memory)
        val test = scope.createTest()

        logger.error  {"Test serialized: ${test}" }
        concreteRunner.run(test)
    }

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
        logger.info { "resolvedArgs: $args" }
        val arrange = decoderApi.arrangeStmts().map { com.google.protobuf.Any.pack(it) }
        val methodCall = decoderApi.callMethod(method, args)
        val resultAsMessage = when (result) {
            is IlMethodResult.Success -> result.result
            is IlMethodResult.Exception -> result.exception
            else -> TODO()
        }.let { resolve(it, method.returnType) }
        val test = ilTest {
            arrangeStmts.addAll(arrange)
            call = com.google.protobuf.Any.pack(methodCall)
            expectedResult = com.google.protobuf.Any.pack(resultAsMessage)
        }
        return test
    }
}
