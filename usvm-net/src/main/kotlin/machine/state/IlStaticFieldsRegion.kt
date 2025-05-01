package org.usvm.machine.state

import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UBoolExpr
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.collections.immutable.getOrDefault
import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.machine.ilctx
import org.usvm.memory.ULValue
import org.usvm.memory.UMemoryRegion
import org.usvm.memory.UMemoryRegionId
import org.usvm.memory.guardedWrite
import org.usvm.sampleUValue

class IlStaticFieldLValue<Sort: USort>(
    val field: IlField,
    override val sort: Sort
): ULValue<IlStaticFieldLValue<Sort>, Sort> {
    override val memoryRegionId: UMemoryRegionId<IlStaticFieldLValue<Sort>, Sort>
        get() = IlStaticFieldsRegionId(sort)
    override val key: IlStaticFieldLValue<Sort>
        get() = this
}

data class IlStaticFieldsRegionId<Sort: USort>(
    override val sort: Sort
): UMemoryRegionId<IlStaticFieldLValue<Sort>, Sort> {
    override fun emptyRegion(): UMemoryRegion<IlStaticFieldLValue<Sort>, Sort> {
        return IlStaticFieldsMemoryRegion(sort, persistentHashMapOf())
    }
}

internal class IlStaticFieldsMemoryRegion<Sort: USort>(
    private val sort: Sort,
    private val fieldValuesByClass: UPersistentHashMap<IlType, UPersistentHashMap<IlField, UExpr<Sort>>>
): UMemoryRegion<IlStaticFieldLValue<Sort>, Sort> {
    override fun read(key: IlStaticFieldLValue<Sort>): UExpr<Sort> {
        return fieldValuesByClass[key.field.declaringType]?.get(key.field) ?: sort.ilctx.mkStaticFieldReading(
            sort,
            key.memoryRegionId as IlStaticFieldsRegionId<Sort>,
            key.field
        )
    }

    override fun write(
        key: IlStaticFieldLValue<Sort>,
        value: UExpr<Sort>,
        guard: UBoolExpr,
        ownership: MutabilityOwnership
    ): UMemoryRegion<IlStaticFieldLValue<Sort>, Sort> {
        val typeFields = fieldValuesByClass.getOrDefault(key.field.declaringType, defaultValue = persistentHashMapOf())
        val newFieldsValues = typeFields.guardedWrite(key.field, value, guard, ownership) { key.sort.sampleUValue() }
        val newStorage = fieldValuesByClass.put(key.field.declaringType, newFieldsValues, ownership)
        return IlStaticFieldsMemoryRegion(sort, newStorage)
    }
}
