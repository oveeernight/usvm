import com.jetbrains.rd.framework.impl.RpcTimeouts
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.Logger
import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.IlSettings
import org.jacodb.api.net.database.IlDatabaseImpl
import org.jacodb.api.net.features.IlMethodInstructionsFeature
import org.jacodb.api.net.generated.models.PublicationRequest
import org.jacodb.api.net.generated.models.ilModel
import org.jacodb.api.net.generated.models.ilSigModel
import org.jacodb.api.net.publication.IlPublicationCache
import org.jacodb.api.net.rdinfra.RdServer

class JacoDBContainer(
    assemblies: List<String>,
    tacBuilderPath: String,
    builder: IlSettings.() -> Unit
) {
    lateinit var publication: IlPublication

    init {
        val settings = IlSettings()
        settings.builder()
        val database = IlDatabaseImpl(settings)
        val freePort = NetUtils.findFreePort(0)
        val server = RdServer(freePort, tacBuilderPath, database)
        server.protocol.scheduler.queue {
            val res =
                server.protocol.ilModel.ilSigModel.publication.sync(
                    PublicationRequest(assemblies),
                    RpcTimeouts.longRunning
                )
            database.persistence.persistAsmHierarchy(res.reachableAsms, res.referencedAsms)
            database.persistence.persistTypes(res.reachableTypes)


            publication = database.publication(
                assemblies,
                listOf(
                    IlPublicationCache(settings.publicationCacheSettings),
                    IlMethodInstructionsFeature(),
                )
            )
        }
        server.close()
    }

    companion object {
        private lateinit var instance: JacoDBContainer

        fun getInstanceOrCreate(
            sourceAsmPath: List<String>,
            tacBuilderPath: String,
            builder: IlSettings.() -> Unit = { }
        ): JacoDBContainer {
            org.usvm.machine.logger.error { sourceAsmPath[0] }
            return if (::instance.isInitialized) {
                instance
            } else {
                JacoDBContainer(sourceAsmPath, tacBuilderPath, builder).also { instance = it }
            }
        }
    }
}
