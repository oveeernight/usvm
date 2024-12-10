import org.example.ilinstances.IlMethod
import org.jacodb.api.net.IlPublication
import java.io.File
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaConstructor

fun getPublicationAssembly(samplesPath: String) : List<File> = TODO()

fun IlPublication.getMethodByName(f: KFunction<*>): IlMethod =
    findIlTypeOrNull(f.declaringClass().name)!!.methods.find { it.name == f.name }!!


fun KFunction<*>.declaringClass(): Class<*> =
    (javaMethod ?: javaConstructor)?.declaringClass!!
