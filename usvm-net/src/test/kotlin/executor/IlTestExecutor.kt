package executor

import common.DecoderApi
import common.IlTestStateResolver
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.machine.IlContext
import org.usvm.machine.state.IlState
import org.usvm.memory.UReadOnlyMemory
import org.usvm.model.UModelBase

class IlTestExecutor(val ctx: IlContext, val state: IlState, val method: IlMethod) {
    private val runner = TestRunner()
    fun execute() {
        val model = state.models.first()
        val memory = state.memory

        val scope = MemoryScope(ctx, method, model, memory)
        val test = scope.createTest()
        runner.run(test)
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
        val instance = resolveThis()
        val args = resolveArgs()
        val arrange = decoderApi.arrangeStmts()
        val methodCall = decoderApi.callMethod(method, listOf(instance) + args)
        return IlTest(arrange, methodCall)
    }
}

class IlTest(
    val arrange: List<IlTestStmt>,
    val callMethod: IlTestExpr
)
