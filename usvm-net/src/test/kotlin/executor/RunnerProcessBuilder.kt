package executor

import IlMethodTestRunner
import org.usvm.machine.logger
import java.io.File

class RunnerProcessBuilder {
    companion object {
        fun build(port: Int): ProcessBuilder {
            logger.info { "Executor server listening on port $port" }
            val executorDir = File(IlMethodTestRunner.executorPath)
            val samplesAsm = IlMethodTestRunner.samplesAsmPath
            val builder = ProcessBuilder()
            val enabled = "1"
            val instrumentMainOnlyConfig = ExecutorEnvironmentConfig(
                coreclrProfiler = "{2800fea6-9667-4b42-a2b6-45dc98e77e9e}",
                coreclrProfilerPath = IlMethodTestRunner.profilerPath,
                coreclrEnableProfiling = enabled,
                instrumentMainOnly = enabled,
            )
            return builder.command("./TestExecutor.Application", "--asm", samplesAsm, "--port", port.toString())
                .directory(executorDir)
                .withEnvironmentConfig(instrumentMainOnlyConfig)
        }
    }
}
