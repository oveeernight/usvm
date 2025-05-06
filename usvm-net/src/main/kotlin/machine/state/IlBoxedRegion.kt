package org.usvm.machine.state

import org.usvm.*
import org.usvm.collection.field.UFieldLValue
import org.usvm.collection.field.UInputFieldId
import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.machine.ilctx
import org.usvm.memory.*
import org.usvm.memory.key.UHeapRefKeyInfo
import sun.jvm.hotspot.oops.CellTypeState.ref

class IlBoxedLocationLValue<Sort: USort>(
    override val sort: Sort,
    val ref: UHeapRef): ULValue<IlBoxedLocationLValue<Sort>, Sort> {
    override val memoryRegionId: UMemoryRegionId<IlBoxedLocationLValue<Sort>, Sort>
        get() = IlBoxedLocationRegionId(sort)
    override val key: IlBoxedLocationLValue<Sort>
        get() = this
}

class IlBoxedLocationRegionId<Sort: USort>(override val sort: Sort): UMemoryRegionId<IlBoxedLocationLValue<Sort>, Sort> {

    override fun emptyRegion(): UMemoryRegion<IlBoxedLocationLValue<Sort>, Sort> {
        return IlBoxedLocationRegion(sort, persistentHashMapOf())
    }
}

class IlInputBoxedValuesId<Sort: USort>(override val sort: Sort): USymbolicCollectionId<UHeapRef, Sort, IlInputBoxedValuesId<Sort> > {
    private fun mkLValue(sort: Sort, ref: UHeapRef) = IlBoxedLocationLValue(sort, ref)

    override fun instantiate(
        collection: USymbolicCollection<IlInputBoxedValuesId<Sort>, UHeapRef, Sort>,
        key: UHeapRef,
        composer: UComposer<*, *>?
    ): UExpr<Sort> {
        if (composer == null) {
            return sort.ilctx.mkBoxedValueReading(key, collection)
        }

        val writableMemory = composer.memory.toWritableMemory(sort.uctx.defaultOwnership)
        collection.applyTo(writableMemory, key, composer)
        return writableMemory.read(mkLValue(sort, key))
    }

    override fun <Type> write(memory: UWritableMemory<Type>, key: UHeapRef, value: UExpr<Sort>, guard: UBoolExpr) =
        memory.write(mkLValue(sort, key), value, guard)

    override fun keyInfo(): USymbolicCollectionKeyInfo<UHeapRef, *> =
        UHeapRefKeyInfo

    override fun emptyRegion(): USymbolicCollection<IlInputBoxedValuesId<Sort>, UHeapRef, Sort> =
        USymbolicCollection(this, UFlatUpdates(keyInfo()))

}

typealias IlInputBoxedValues<Sort> = USymbolicCollection<IlInputBoxedValuesId<Sort>, UHeapRef, Sort>

class IlBoxedLocationRegion<Sort: USort>(
    private val sort: Sort,
    private val allocatedValues: UPersistentHashMap<UConcreteHeapAddress, UExpr<Sort>>,
    private var inputValues: IlInputBoxedValues<Sort>? = null,
): UMemoryRegion<IlBoxedLocationLValue<Sort>, Sort> {
    override fun read(key: IlBoxedLocationLValue<Sort>): UExpr<Sort> =
        key.ref.mapWithStaticAsSymbolic(
            concreteMapper = { concreteRef -> allocatedValues[concreteRef.address]!!},
            symbolicMapper = { inputRef -> inputValues?.read(inputRef)!! }
        )

    private fun updateAllocated(allocatedValues: UPersistentHashMap<UConcreteHeapAddress, UExpr<Sort>>) =
        IlBoxedLocationRegion(sort, allocatedValues, inputValues)

    private fun updateInput(inputValues: IlInputBoxedValues<Sort>) =
        IlBoxedLocationRegion(sort, allocatedValues, inputValues)

    private fun getInputValues(): IlInputBoxedValues<Sort> {
        if (inputValues == null)
            inputValues = IlInputBoxedValuesId(sort).emptyRegion()
        return inputValues!!
    }

    override fun write(
        key: IlBoxedLocationLValue<Sort>,
        value: UExpr<Sort>,
        guard: UBoolExpr,
        ownership: MutabilityOwnership
    ): UMemoryRegion<IlBoxedLocationLValue<Sort>, Sort> = foldHeapRefWithStaticAsSymbolic(key.ref,
        initial = this,
        initialGuard = guard,
        blockOnConcrete = { _, (ref, guard) ->
            val newAllocated =
                allocatedValues.guardedWrite(ref.address, value, guard, ownership) { sort.sampleUValue() }
            updateAllocated(newAllocated)
        },
        blockOnSymbolic = { _, (ref, guard) ->
            val newInputValues = getInputValues().write(ref, value, guard, ownership)
            updateInput(newInputValues)
        }
    )
}
