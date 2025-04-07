package org.usvm.machine

import kotlinx.coroutines.runBlocking
import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.features.InMemoryIlHierarchyReq
import org.jacodb.api.net.features.InMemoryIlHierarchy
import org.jacodb.api.net.features.query
import org.jacodb.api.net.ilinstances.ext.isAssignableTo
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlArrayType
import org.jacodb.api.net.ilinstances.impl.IlClassType
import org.jacodb.api.net.ilinstances.impl.IlStructType
import org.jacodb.api.net.ilinstances.impl.IlValueType
import org.jacodb.api.net.publication.IlPredefinedAsmExt.mscorelib
import org.usvm.types.USupportTypeStream
import org.usvm.types.UTypeStream
import org.usvm.types.UTypeSystem
import kotlin.time.Duration


// TODO fix when class hierarchy will be ready
class IlTypeSystem(private val publication: IlPublication): UTypeSystem<IlType> {
    private val arrayClassId = publication.mscorelibClassId("System.Array")
    private val valueTypeClassId = publication.mscorelibClassId("System.ValueType")
    private val objectClassId = publication.mscorelibClassId("System.Object")

    override val typeOperationsTimeout: Duration
        get() = Duration.INFINITE

    private val topTypeStream by lazy {
        publication.findIlTypeOrNull(objectClassId)!!.let { USupportTypeStream.from(this, it) }
    }
    override fun topTypeStream(): UTypeStream<IlType> {
       return topTypeStream
    }

    // TODO: generics, arrays
    override fun findSubtypes(type: IlType): Sequence<IlType> {
        val request = InMemoryIlHierarchyReq(type.id, true)
        return runBlocking {
            publication.query(InMemoryIlHierarchy, request)
        }
    }

    override fun isInstantiable(type: IlType): Boolean =
        !type.isAbstract
        && !type.isInterface

    override fun isFinal(type: IlType): Boolean {
        // TODO: `isSealed` reflection property
        return false
    }

    override fun hasCommonSubtype(type: IlType, types: Collection<IlType>): Boolean = when {
        type is IlArrayType -> types.all {
            // TODO: Array class interfaces
            it.id == type.id
            || it.id == arrayClassId
            || it.id == valueTypeClassId
        }

        type is IlStructType -> {
            // TODO: without building set?
            val interfaces = type.interfaces.toSet()
            types.all {
                if (it.isInterface) interfaces.contains(it)
                else
                    it.id == type.id
                    || it.id == valueTypeClassId
            }
        }

        // TODO: use `isValueType` reflection property
        type.isInterface -> types.none { it is IlValueType }

        type is IlClassType -> types.all {
            it.isInterface || isSupertype(it, type)
        }

        else -> error("Unexpected type: $type")
    }

    override fun isSupertype(supertype: IlType, type: IlType): Boolean = type.isAssignableTo(supertype)

    companion object {
        private fun IlPublication.mscorelibClassId(name: String) = TypeId(
            listOf(),
            mscorelib(),
            name,
        )
    }
}
