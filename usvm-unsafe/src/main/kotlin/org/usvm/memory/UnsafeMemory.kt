package org.usvm.memory

import org.jacodb.api.common.CommonType
import org.usvm.UBvSort
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort

import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.constraints.UTypeConstraints
import org.usvm.UIndexedMocker


class UnsafeLValue<Sort: USort>(
    val base: UExpr<Sort>,
    val offset: UExpr<UBvSort>,
    val sightType: CommonType
)


abstract class UnsafeMemory<Type, Method>(
    val ctx: UContext<*>,
    ownership: MutabilityOwnership,
    types: UTypeConstraints<Type>,
    stack: URegistersStack = URegistersStack(),
    mocks: UIndexedMocker<Method> = UIndexedMocker(),
    regions: UPersistentHashMap<UMemoryRegionId<*, *>, UMemoryRegion<*, *>> = persistentHashMapOf()
) : UMemory<Type, Method>(ctx, ownership, types, stack, mocks, regions) {
    abstract fun <Sort: USort> readUnsafe(lvalue: UnsafeLValue<Sort>): UExpr<Sort>
    abstract fun <Sort: USort> writeUnsafe(lvalue: UnsafeLValue<Sort>, value: UExpr<Sort>)
    abstract override fun clone(
        typeConstraints: UTypeConstraints<Type>,
        thisOwnership: MutabilityOwnership,
        cloneOwnership: MutabilityOwnership,
    ): UnsafeMemory<Type, Method>
}
