package org.usvm.machine

import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlReferenceType
import org.jacodb.api.net.ilinstances.impl.IlValueType
import org.usvm.*
import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.constraints.UEqualityConstraints
import org.usvm.constraints.UTypeConstraints
import org.usvm.memory.mapWithStaticAsConcrete
import org.usvm.types.UTypeRegion

class IlTypeConstraints(
    ownership: MutabilityOwnership,
    override val typeSystem: IlTypeSystem,
    equalityConstraints: UEqualityConstraints,
    concreteRefToType: UPersistentHashMap<UConcreteHeapAddress, IlType> = persistentHashMapOf(),
    symbolicRefToTypeRegion: UPersistentHashMap<USymbolicHeapRef, UTypeRegion<IlType>> = persistentHashMapOf()
) : UTypeConstraints<IlType>(ownership, typeSystem, equalityConstraints, concreteRefToType, symbolicRefToTypeRegion) {
    override fun evalIsSubtype(ref: UHeapRef, supertype: IlType): UBoolExpr =
        ref.mapWithStaticAsConcrete(
            concreteMapper = { concreteRef ->
                val concreteType = concreteRefToType[concreteRef.address]!!
                if (typeSystem.isSupertype(supertype, concreteType)) {
                    concreteRef.ctx.trueExpr
                } else {
                    concreteRef.ctx.falseExpr
                }
            },
            symbolicMapper = mapper@{ symbolicRef ->
                if (symbolicRef == symbolicRef.uctx.nullRef && supertype is IlReferenceType) {
                    // accordingly to the [UIsSubtypeExpr] specification, [nullRef] always satisfies the [type]
                    return@mapper symbolicRef.ctx.trueExpr
                }

                if (symbolicRef == symbolicRef.uctx.nullRef && supertype is IlValueType) {
                    return@mapper symbolicRef.ctx.falseExpr
                }
                val typeRegion = getTypeRegion(symbolicRef)

                if (typeRegion.addSupertype(supertype).isEmpty) {
                    symbolicRef.uctx.mkEq(symbolicRef, symbolicRef.uctx.nullRef)
                } else {
                    symbolicRef.uctx.mkIsSubtypeExpr(symbolicRef, supertype)
                }
            },
            ignoreNullRefs = false
        )

    override fun clone(
        equalityConstraints: UEqualityConstraints,
        thisOwnership: MutabilityOwnership,
        cloneOwnership: MutabilityOwnership
    ): UTypeConstraints<IlType> =
        IlTypeConstraints(
            cloneOwnership,
            typeSystem,
            equalityConstraints,
            concreteRefToType,
            symbolicRefToTypeRegion
        ).also { this.ownership = thisOwnership }
}
