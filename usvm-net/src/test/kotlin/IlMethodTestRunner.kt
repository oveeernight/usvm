import executor.IlTestExecutor
import executor.IlMethodTestRunnerController
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.usvm.UMachineOptions
import org.usvm.machine.IlMachine
import org.usvm.machine.IlMachineOptions
import org.usvm.machine.logger
import org.usvm.machine.state.IlState
import org.usvm.test.util.TestRunner
import testrunner.expressions.TestExpressions
import testrunner.expressions.TestExpressions.ExecutionResult
import testrunner.expressions.executionResult
import testrunner.expressions.success
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.time.measureTime


@ExtendWith(IlMethodTestRunnerController::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class IlMethodTestRunner : TestRunner<ExecutionResult, KFunction<*>, KClass<*>, IlTypeCoverage>() {

    private val container by lazy { JacoDBContainer.getInstanceOrCreate(listOf(samplesAsmPath), tacBuilderPath) }
    protected val executor by lazy { IlTestExecutor() }

    override val typeTransformer: (Any?) -> KClass<*>
        get() = TODO("Not yet implemented")
    override val checkType: (KClass<*>, KClass<*>) -> Boolean
        get() = TODO("Not yet implemented")
    override val runner: (KFunction<*>, UMachineOptions) -> List<ExecutionResult>
        get() = { method, options ->
            val publication = container.publication
            val ilMethod = publication.getMethodByName(method)
            val ilOptions = IlMachineOptions()
            val machine = IlMachine(publication, options, ilOptions)
            var analysisResult: List<IlState> = emptyList()
            val analysisTime = measureTime {
                try {
                    analysisResult = machine.analyze(listOf(ilMethod))
                } catch (e: Exception) {

                }
            }
//            val path = "/home/rnpozharskiy/study/bench/usvm/arith5.txt"
//            File(path).appendText("${analysisTime.inWholeSeconds}:${analysisTime.inWholeMilliseconds % 1000}\n")

//            listOf(executionResult { this.success = success {} })
            if (analysisResult.isEmpty()) {
                error("Failed analysis for $method")
            }
            val check = executor.execute(analysisResult, ilMethod)
            when (check.resultCase) {
                TestExpressions.ExecutionResult.ResultCase.SUCCESS -> {
                    val success = check.success
                    logger.info { "$ilMethod: successfully generated ${success.generatedTests} tests with total coverage ${success.coverage}" }
                }
                TestExpressions.ExecutionResult.ResultCase.FAIL -> {
                    val fail = check.fail
                    logger.error { "$ilMethod: failed. Reproduced {${fail.reproduced} tests with total coverage ${fail.coverage}. Reason:\n${fail.reason}" }
                    error { "$ilMethod: failed. Reproduced {${fail.reproduced} tests with total coverage ${fail.coverage}. Reason:\n${fail.reason}" }
                }
                else -> error("unreachable")
            }
            listOf(check)
        }
    protected val runnerWithDefaultOptions: (KFunction<*>) -> Unit = { f -> runner(f, options)}
    override val coverageRunner: (List<ExecutionResult>) -> IlTypeCoverage
        get() = TODO("Not yet implemented")
    override var options: UMachineOptions = UMachineOptions()

    companion object {
        private val dir = System.getProperty("user.dir")
        val samplesAsmPath = Paths.get(dir, "src/test/dotnet/samples/bin/Release/net7.0/publish/samples.dll").pathString
        val samplesAsmName = "samples, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null"
        val tacBuilderPath: String = "../../dotnet-tac/TACBuilder/bin/Release/net8.0/"
        val executorPath: String = "../../test-executor/TestExecutor.Application/bin/Release/net8.0"
        val profilerPath: String = "/home/rnpozharskiy/work/test-executor/TestExecutor.Application/bin/Release/net8.0/libvsharpCoverage.so"
    }
}

class IlTypeCoverage
