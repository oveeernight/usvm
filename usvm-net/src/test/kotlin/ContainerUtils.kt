import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlMethod
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaConstructor

//fun getPublicationAssembly(samplesPath: String) : List<File> = TODO()

fun IlPublication.getMethodByName(f: KFunction<*>): IlMethod{
    val typeName = f.declaringClass.name
    val typeId = TypeId(typeName = typeName, asmName = IlMethodTestRunner.samplesAsmName, typeArgs = emptyList())
    return findIlTypeOrNull(typeId)!!.methods.find { it.name.lowercase() == f.name.lowercase() }!!
}


private val KFunction<*>.declaringClass: Class<*>  get() = (javaMethod ?: javaConstructor)?.declaringClass!!
