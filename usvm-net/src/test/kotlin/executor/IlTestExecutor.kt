package executor

import common.IlTestStateResolver
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

        logger.info  {"Test serialized: ${prettyJson.encodeToString(test)}" }
//        concreteRunner.run(test)
    }

}


private class MemoryScope(
    ctx: IlContext,
    method: IlMethod,
    model: UModelBase<IlType>,
    stateMemory: UReadOnlyMemory<IlType>
) : IlTestStateResolver<IlTestExpr>(ctx, method, model, stateMemory) {
    override val decoderApi: IlTestExecutorDecoderApi = IlTestExecutorDecoderApi(ctx)

    fun createTest(): IlTest {
//        val instance = resolveThis()
        val args = resolveArgs()
        val arrange = decoderApi.arrangeStmts()
        val methodCall = decoderApi.callMethod(method, args)
        return IlTest(arrange, methodCall)
    }
}

@Serializable
class IlTest(val arrange: List<IlTestStmt>, val callMethod: IlTestExpr)

@ExperimentalSerializationApi
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = " "
}
