package org.usvm.machine.state.boxed

import org.usvm.UExpr
import org.usvm.UHeapRef
import org.usvm.USort
import org.usvm.memory.UReadOnlyMemoryRegion
import org.usvm.model.UModelEvaluator
import org.usvm.model.modelEnsureConcreteInputRef
import org.usvm.solver.UCollectionDecoder

class BoxedRegionLazyModel<Sort : USort>(
    private val model: UModelEvaluator<*>,
    private val inputValuesDecoder: UCollectionDecoder<UHeapRef, Sort>
) : UReadOnlyMemoryRegion<IlBoxedLocationLValue<Sort>, Sort> {

    private val inputValues: UReadOnlyMemoryRegion<UHeapRef, Sort> = inputValuesDecoder.decodeCollection(model)

    override fun read(key: IlBoxedLocationLValue<Sort>): UExpr<Sort> {
        val ref = modelEnsureConcreteInputRef(key.ref)
        return inputValues.read(ref)
    }

}
