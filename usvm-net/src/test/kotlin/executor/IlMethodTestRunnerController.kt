package executor

import JacoDBContainer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL

class IlMethodTestRunnerController(): BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private var started = false

    override fun beforeAll(context: ExtensionContext) {
        if (!started) {
            started = true
            context.root.getStore(GLOBAL).put("runner callback", this)
        }
    }

    override fun close() {
        if (ConcreteTestRunnerContainer.isInitialized) {
            ConcreteTestRunnerContainer.runner.close()
        }
        JacoDBContainer.getInstance().server.close()
    }
}
