import org.usvm.UMachineOptions
import org.usvm.test.util.TestRunner
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class IlMethodTestRunner : TestRunner<IlTest, KFunction<*>, KClass<*>, IlTypeCoverage>() {

    private val publication by lazy { JacoDBContainer.getInstanceOrCreate(assemblies, tacBuilderPath) }

    override val typeTransformer: (Any?) -> KClass<*>
        get() = TODO("Not yet implemented")
    override val checkType: (KClass<*>, KClass<*>) -> Boolean
        get() = TODO("Not yet implemented")
    override val runner: (KFunction<*>, UMachineOptions) -> List<IlTest>
        get() = TODO("Not yet implemented")
    override val coverageRunner: (List<IlTest>) -> IlTypeCoverage
        get() = TODO("Not yet implemented")
    override var options: UMachineOptions
        get() = TODO("Not yet implemented")
        set(value) {}

    companion object {
        private val samples = "usvm.usvm-net.test.samples"
        private val tacBuilderPath: String = TODO()

        private val assemblies: List<File> by lazy {
            getPublicationAssembly(samples)
        }
    }
}
