package org.usvm.machine.state.boxed

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.mkConst
import org.usvm.*
import org.usvm.machine.IlTranslator
import org.usvm.memory.URangedUpdateNode
import org.usvm.memory.UReadOnlyMemoryRegion
import org.usvm.memory.USymbolicCollection
import org.usvm.model.UModelEvaluator
import org.usvm.solver.U1DUpdatesTranslator
import org.usvm.solver.UCollectionDecoder
import org.usvm.solver.URegionDecoder
import org.usvm.solver.URegionTranslator
import java.util.IdentityHashMap


class IlBoxedRegionDecoder<Sort : USort>(
    private val regionId: IlBoxedLocationRegionId<Sort>,
    private val translator: IlTranslator
) : URegionDecoder<IlBoxedLocationLValue<Sort>, Sort> {
    private var inputRegionTranslator: IlInputBoxedValuesRegionTranslator<Sort>? = null

    fun inputBoxedValuesTranslator(collectionId: IlInputBoxedValuesId<Sort>): URegionTranslator<IlInputBoxedValuesId<Sort>, UHeapRef, Sort> {
        if (inputRegionTranslator == null) {
            inputRegionTranslator = IlInputBoxedValuesRegionTranslator(collectionId, translator)
        }
        return inputRegionTranslator!!
    }

    override fun decodeLazyRegion(
        model: UModelEvaluator<*>,
        assertions: List<KExpr<KBoolSort>>
    ): UReadOnlyMemoryRegion<IlBoxedLocationLValue<Sort>, Sort>? =
        inputRegionTranslator?.let { BoxedRegionLazyModel(model, it) }

}

private class IlInputBoxedValuesRegionTranslator<Sort : USort>(
    private val collectionId: IlInputBoxedValuesId<Sort>,
    translator: IlTranslator
) : URegionTranslator<IlInputBoxedValuesId<Sort>, UHeapRef, Sort>, UCollectionDecoder<UHeapRef, Sort> {
    private val initialValue = with(collectionId.sort.uctx) {
        mkArraySort(addressSort, collectionId.sort).mkConst(collectionId.toString())
    }

    private val visitorCache = IdentityHashMap<Any?, KExpr<KArraySort<UAddressSort, Sort>>>()
    private val updatesTranslator = IlInputBoxedValueUpdatesTranslator(translator, initialValue)


    override fun translateReading(
        region: USymbolicCollection<IlInputBoxedValuesId<Sort>, UHeapRef, Sort>,
        key: UHeapRef
    ): KExpr<Sort> {
        val translatedCollection = region.updates.accept(updatesTranslator, visitorCache)
        return updatesTranslator.visitSelect(translatedCollection, key)
    }

    override fun decodeCollection(model: UModelEvaluator<*>): UReadOnlyMemoryRegion<UHeapRef, Sort> {
        return model.evalAndCompleteArray1DMemoryRegion(initialValue.decl)
    }
}

private class IlInputBoxedValueUpdatesTranslator<Sort: USort>(translator: IlTranslator, initialValue: KExpr<KArraySort<UAddressSort, Sort>>): U1DUpdatesTranslator<UAddressSort, Sort>(translator, initialValue) {
    override fun KContext.translateRangedUpdate(
        previous: KExpr<KArraySort<UAddressSort, Sort>>,
        update: URangedUpdateNode<*, *, UExpr<UAddressSort>, Sort>
    ): KExpr<KArraySort<UAddressSort, Sort>> {
        TODO("boxed values has no ranged updates")
    }
}
