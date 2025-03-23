package org.usvm.machine

import org.jacodb.api.net.IlPublication
import org.jacodb.api.net.generated.models.TypeId
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.publication.IlPredefinedAsmExt.mscorelib
import org.usvm.types.USupportTypeStream
import org.usvm.types.UTypeStream
import org.usvm.types.UTypeSystem
import kotlin.time.Duration


// TODO fix when class hierarchy will be ready
class IlTypeSystem(private val publication: IlPublication): UTypeSystem<IlType> {
    override val typeOperationsTimeout: Duration
        get() = TODO("Not yet implemented")

    private val topTypeStream by lazy {
        val objectTypeId = TypeId(typeName = "System.Object", asmName = publication.mscorelib(), typeArgs = emptyList())
        publication.findIlTypeOrNull(objectTypeId)!!.let { USupportTypeStream.from(this, it) } }
    override fun topTypeStream(): UTypeStream<IlType> {
       return topTypeStream
    }

    override fun findSubtypes(type: IlType): Sequence<IlType> {
        return emptySequence()
    }

    override fun isInstantiable(type: IlType): Boolean {
        return false
    }

    override fun isFinal(type: IlType): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasCommonSubtype(type: IlType, types: Collection<IlType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSupertype(supertype: IlType, type: IlType): Boolean {
        TODO("Not yet implemented")
    }
}
