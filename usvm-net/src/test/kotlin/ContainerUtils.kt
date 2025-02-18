import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.ilinstances.IlMethod
import java.io.File
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaConstructor

//fun getPublicationAssembly(samplesPath: String) : List<File> = TODO()

fun IlPublication.getMethodByName(f: KFunction<*>): IlMethod{
    val name = f.declaringClass.name
    return findIlTypeOrNull(name)!!.methods.find { it.name.lowercase() == f.name.lowercase() }!!
}


private val KFunction<*>.declaringClass: Class<*>  get() = (javaMethod ?: javaConstructor)?.declaringClass!!
