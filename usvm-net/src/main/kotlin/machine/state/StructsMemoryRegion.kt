package org.usvm.machine.state

import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlField
import org.usvm.UBoolExpr
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.machine.IlStruct
import org.usvm.memory.ULValue
import org.usvm.memory.UMemoryRegion
import org.usvm.memory.UMemoryRegionId

class StructFieldLValue<Key, Sort : USort>(
    override val sort: Sort,
    val structRegion: UMemoryRegion<Key, Sort>,
    val structKey: Key,
    val field: IlField
) : ULValue<StructFieldLValue<Key, Sort>, Sort> {
    override val memoryRegionId: UMemoryRegionId<StructFieldLValue<Key, Sort>, Sort> by lazy {
        StructsRegionId(
            sort,
            structRegion,
            structKey,
            field
        )
    }
    override val key: StructFieldLValue<Key, Sort>
        get() = this
}

class StructsRegionId<Key, Sort : USort>(
    override val sort: Sort,
    val structRegion: UMemoryRegion<Key, Sort>,
    val structKey: Key,
    val field: IlField
) : UMemoryRegionId<StructFieldLValue<Key, Sort>, Sort> {
    override fun emptyRegion(): UMemoryRegion<StructFieldLValue<Key, Sort>, Sort> =
        StructsMemoryRegion(sort, structRegion, structKey, field)
}

class StructsMemoryRegion<Key, Sort : USort>(
    private val sort: Sort,
    val structRegion: UMemoryRegion<Key, Sort>,
    private val structKey: Key,
    private val field: IlField
) : UMemoryRegion<StructFieldLValue<Key, Sort>, Sort> {
    override fun read(key: StructFieldLValue<Key, Sort>): UExpr<Sort> {
        val struct = structRegion.read(structKey) as IlStruct
        return struct.fields[field].cast()
    }

    override fun write(
        key: StructFieldLValue<Key, Sort>,
        value: UExpr<Sort>,
        guard: UBoolExpr,
        ownership: MutabilityOwnership
    ): UMemoryRegion<StructFieldLValue<Key, Sort>, Sort> {
        val struct = structRegion.read(structKey) as IlStruct
        val updated = struct.writeField(field, value, ownership)
        val updatedStructRegion = structRegion.write(structKey, updated.cast(), guard, ownership)
        return StructsMemoryRegion(sort, updatedStructRegion, structKey, field)
    }


}
