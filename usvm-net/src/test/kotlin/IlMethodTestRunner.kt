import executor.IlTestExecutor
import org.usvm.UMachineOptions
import org.usvm.machine.IlMachine
import org.usvm.machine.IlMachineOptions
import org.usvm.test.util.TestRunner
import testrunner.expressions.TestExpressions
import testrunner.expressions.TestExpressions.ExecutionResult
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

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
            val res = machine.analyze(listOf(ilMethod)).let {
                executor.execute(it, ilMethod)
            }
            listOf(res)
        }
    override val coverageRunner: (List<ExecutionResult>) -> IlTypeCoverage
        get() = TODO("Not yet implemented")
    override var options: UMachineOptions = UMachineOptions()

    companion object {
        private val dir = System.getProperty("user.dir")
        val samplesAsmPath = Paths.get(dir, "src/test/dotnet/samples/bin/Release/net7.0/publish/samples.dll").pathString
        val samplesAsmName = "samples, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null"
        val tacBuilderPath: String = "../../dotnet-tac/TACBuilder/bin/Release/net8.0/linux-x64/publish"
        val executorPath: String = "../../test-executor/TestExecutor.Application/bin/Debug/net8.0"
        val profilerPath: String = "/home/rnpozharskiy/work/test-executor/TestExecutor.CoverageTool/bin/Debug/net8.0/libvsharpCoverage.so"
//        private val assemblies: List<File> by lazy {
//            getPublicationAssembly(samplesAsm)
//        }
    }
}
