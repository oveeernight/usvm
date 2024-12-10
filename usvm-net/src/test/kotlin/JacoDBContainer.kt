import org.jacodb.api.net.IlDatabase
import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.IlSettings
import org.jacodb.api.net.database.IlDatabaseImpl
import org.jacodb.api.net.rdinfra.NetApiServer
import java.io.File

class JacoDBContainer(
    assemblies: List<File>,
    tacBuilderPath: String,
    builder: IlSettings.() -> Unit
) {
    val publication: IlPublication = TODO()

    init {
        TODO()
//        val settings = IlSettings()
//        settings.builder()
//        val db = IlDatabaseImpl(settings)
//        val api = NetApiServer(tacBuilderPath, sourceAsmPath, db)
//        api.requestTestAsm()
//        val publication = db.typeLoader()
//        api.close()
//        this.publication = publication
    }

    companion object {
        private lateinit var instance: JacoDBContainer

        fun getInstanceOrCreate(
            sourceAsmPath: List<File>,
            tacBuilderPath: String,
            builder: IlSettings.() -> Unit = { }
        ): JacoDBContainer {
            return if (!::instance.isInitialized) {
                instance
            } else {
                JacoDBContainer(sourceAsmPath, tacBuilderPath, builder).also { instance = it }
            }
        }


    }
}
