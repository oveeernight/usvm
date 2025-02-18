package org.usvm.machine

import org.jacodb.api.net.ilinstances.IlType
import org.usvm.types.UTypeStream
import org.usvm.types.UTypeSystem
import kotlin.time.Duration

class IlTypeSystem: UTypeSystem<IlType> {
    override val typeOperationsTimeout: Duration
        get() = TODO("Not yet implemented")

    override fun topTypeStream(): UTypeStream<IlType> {
        TODO("Not yet implemented")
    }

    override fun findSubtypes(type: IlType): Sequence<IlType> {
        TODO("Not yet implemented")
    }

    override fun isInstantiable(type: IlType): Boolean {
        TODO("Not yet implemented")
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
