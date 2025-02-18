package executor

import com.google.protobuf.Message
import common.IlTestStateResolver
import kotlinx.serialization.ExperimentalSerializationApi
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.machine.IlContext
import org.usvm.machine.logger
import org.usvm.machine.state.IlState
import org.usvm.memory.UReadOnlyMemory
import org.usvm.model.UModelBase

class IlTestExecutor(val state: IlState, val method: IlMethod) {
//    private val concreteRunner = ConcreteTestRunner()
    @OptIn(ExperimentalSerializationApi::class)
    fun execute() {
        val model = state.models.first()
        val memory = state.memory
        val scope = MemoryScope(state.ctx, method, model, memory)
        val test = scope.createTest()

        logger.info  {"Test serialized: ${test}" }
//        concreteRunner.run(test)
    }

}


private class MemoryScope(
    ctx: IlContext,
    method: IlMethod,
    model: UModelBase<IlType>,
    stateMemory: UReadOnlyMemory<IlType>
) : IlTestStateResolver<Message>(ctx, method, model, stateMemory) {
    override val decoderApi: IlTestExecutorDecoderApi = IlTestExecutorDecoderApi(ctx)

    fun createTest(): IlTest {
//        val instance = resolveThis()
        val args = resolveArgs()
        val arrange = decoderApi.arrangeStmts()
        val methodCall = decoderApi.callMethod(method, args)
        println(arrange.toString())
        println(methodCall.toString())
        return IlTest(arrange, methodCall)
    }
}

class IlTest(val arrange: List<Message>, val callMethod: Message)

//@ExperimentalSerializationApi
//private val prettyJson = Json {
//    prettyPrint = true
//    prettyPrintIndent = " "
//}
