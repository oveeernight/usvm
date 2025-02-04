import executor.IlTestExecutor
import org.usvm.UMachineOptions
import org.usvm.machine.IlMachine
import org.usvm.machine.IlMachineOptions
import org.usvm.test.util.TestRunner
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

open class IlMethodTestRunner : TestRunner<IlTest, KFunction<*>, KClass<*>, IlTypeCoverage>() {

    private val container by lazy { JacoDBContainer.getInstanceOrCreate(listOf(samplesAsmPath), tacBuilderPath) }

    override val typeTransformer: (Any?) -> KClass<*>
        get() = TODO("Not yet implemented")
    override val checkType: (KClass<*>, KClass<*>) -> Boolean
        get() = TODO("Not yet implemented")
    override val runner: (KFunction<*>, UMachineOptions) -> List<IlTest>
        get() = { method, options ->
            val publication = container.publication
            val ilMethod = publication.getMethodByName(method)
            val ilOptions = IlMachineOptions()
            val machine = IlMachine(publication, options, ilOptions)
            val states = machine.analyze(listOf(ilMethod))

            for (state in states) {
                val executor = IlTestExecutor(state, ilMethod)
                executor.execute()
            }
        }
        get() = TODO("Not yet implemented")
    override val coverageRunner: (List<IlTest>) -> IlTypeCoverage
        get() = TODO("Not yet implemented")
    override var options: UMachineOptions = UMachineOptions()

    /** Executes method symbolically and concretely with specific dotnet app
     */
    protected fun runMethod(method: KFunction<*>) {

    }

    companion object {
        val samplesAsmPath = "/home/rnpozharskiy/work/usvm/usvm-net/src/test/dotnet/samples/bin/Release/net8.0/publish/samples.dll"
        val samplesAsmName = "samples, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null"
        val tacBuilderPath: String = "/home/rnpozharskiy/work/dotnet-tac/TACBuilder/bin/Release/net8.0/linux-x64/"
        val executorPath: String = "/home/rnpozharskiy/work/test-executor/Application/bin/Debug/net8.0"
//        private val assemblies: List<File> by lazy {
//            getPublicationAssembly(samplesAsm)
//        }
    }
}
