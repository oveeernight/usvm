package executor

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties


@EnvironmentConfiguration
class ExecutorEnvironmentConfig(
    @EnvironmentVariable("CORECLR_PROFILER") val coreclrProfiler: String,
    @EnvironmentVariable("CORECLR_PROFILER_PATH") val coreclrProfilerPath: String,
    @EnvironmentVariable("CORECLR_ENABLE_PROFILING") val coreclrEnableProfiling: String,
    @EnvironmentVariable("COVERAGE_TOOL_INSTRUMENT_MAIN_ONLY") val instrumentMainOnly: String,
    @EnvironmentVariable("COVERAGE_TOOL_RESULT_NAME") val resultName: String,
)

@Suppress("UNCHECKED_CAST")
fun ProcessBuilder.withEnvironmentConfig(c: ExecutorEnvironmentConfig) : ProcessBuilder {
    val env = environment()
    val configClass = c::class as KClass<ExecutorEnvironmentConfig>
    configClass.declaredMemberProperties.forEach {
        val annotations = it.annotations.filterIsInstance<EnvironmentVariable>()
        assert(annotations.size == 1)
        val envVarName = annotations[0].name
        env[envVarName] = it.get(c) as String
    }
    return this
}

@Target(AnnotationTarget.CLASS)
annotation class EnvironmentConfiguration

@Target(AnnotationTarget.PROPERTY)
annotation class EnvironmentVariable(val name: String)
