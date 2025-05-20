package org.usvm.memory

import org.usvm.UBvSort
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort

import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.constraints.UTypeConstraints
import org.usvm.UIndexedMocker

interface AffectedValue<Type, Sort: USort> {
    val value: UExpr<out USort>
    val valueType: Type
    val start: UExpr<UBvSort>
    val end: UExpr<UBvSort>
}


interface UnsafeLValue<Type> {
    val base: ULValue<*, *>?
    val baseType: Type
    val offset: UExpr<UBvSort>
    val sightType: Type
}

abstract class UnsafeMemory<Type, Method>(
    ctx: UContext<*>,
    ownership: MutabilityOwnership,
    types: UTypeConstraints<Type>,
    stack: URegistersStack = URegistersStack(),
    mocks: UIndexedMocker<Method> = UIndexedMocker(),
    regions: UPersistentHashMap<UMemoryRegionId<*, *>, UMemoryRegion<*, *>> = persistentHashMapOf()
) : UMemory<Type, Method>(ctx, ownership, types, stack, mocks, regions) {
    abstract fun readUnsafe(lvalue: UnsafeLValue<Type>): UExpr<out USort>
    abstract fun writeUnsafe(lvalue: UnsafeLValue<Type>, value: UExpr<out USort>, valueType: Type)
    abstract override fun clone(
        typeConstraints: UTypeConstraints<Type>,
        thisOwnership: MutabilityOwnership,
        cloneOwnership: MutabilityOwnership,
    ): UnsafeMemory<Type, Method>
}
