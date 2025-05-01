package org.usvm.machine

import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.mkConst
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.machine.state.IlStaticFieldLValue
import org.usvm.machine.state.IlStaticFieldsRegionId
import org.usvm.memory.UReadOnlyMemory
import org.usvm.memory.UReadOnlyMemoryRegion
import org.usvm.model.UModelEvaluator
import org.usvm.org.usvm.expressions.UnsafeComposer
import org.usvm.org.usvm.expressions.UnsafeTransformer
import org.usvm.org.usvm.expressions.UnsafeTranslator
import org.usvm.solver.URegionDecoder

interface IlTransformer : UnsafeTransformer<IlType, USizeSort> {
    fun <Sort: USort> transform(ref: IlManagedRef<Sort>): UExpr<UAddressSort>
    fun <Sort: USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort>
    fun <Sort: USort> transform(sfreading: IlStaticFieldReading<Sort>): UExpr<Sort>
}

class IlComposer(ctx: UContext<USizeSort>, memory: UReadOnlyMemory<IlType>, ownership: MutabilityOwnership) :
    UnsafeComposer<IlType, USizeSort>(ctx, memory, ownership), IlTransformer {
    override fun <Sort : USort> transform(ref: IlManagedRef<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort: USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort : USort> transform(sfreading: IlStaticFieldReading<Sort>): UExpr<Sort> {
        return memory.read(IlStaticFieldLValue(sfreading.field, sfreading.sort))
    }
}

class IlTranslator(ctx: UContext<USizeSort>) : IlTransformer, UnsafeTranslator<IlType, USizeSort>(ctx) {
    override fun <Sort : USort> transform(ref: IlManagedRef<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort : USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort : USort> transform(sfreading: IlStaticFieldReading<Sort>): UExpr<Sort> =
        getOrPutRegionDecoder(sfreading.regionId) {
            IlStaticFieldDecoder(sfreading.regionId, this)
        }.translate(sfreading)
}

class IlStaticFieldDecoder<Sort: USort>(
    private val regionId: IlStaticFieldsRegionId<Sort>,
    private val translator: IlTranslator,
): URegionDecoder<IlStaticFieldLValue<Sort>, Sort> {
    private val translated = mutableMapOf<IlField, UExpr<Sort>>()

    fun translate(expr: IlStaticFieldReading<Sort>): UExpr<Sort> =
        translated.getOrPut(expr.field) {
            expr.sort.mkConst("${expr.field.declaringType}_${regionId.sort}_${expr.field.name}")
        }

    override fun decodeLazyRegion(
        model: UModelEvaluator<*>,
        assertions: List<KExpr<KBoolSort>>
    ): UReadOnlyMemoryRegion<IlStaticFieldLValue<Sort>, Sort> =
        IlStaticFieldModel(model, translated, translator)
}

class IlStaticFieldModel<Sort: USort>(
    private val model: UModelEvaluator<*>,
    private val translatedFields: Map<IlField, UExpr<Sort>>,
    private val translator: IlTranslator
): UReadOnlyMemoryRegion<IlStaticFieldLValue<Sort>, Sort> {
    override fun read(key: IlStaticFieldLValue<Sort>): UExpr<Sort> {
        val translated = translatedFields[key.field]
            ?: translator.transform(
                key.sort.ilctx.mkStaticFieldReading(
                    key.sort,
                    key.memoryRegionId as IlStaticFieldsRegionId<Sort>,
                    key.field
                )
            )
        return model.evalAndComplete(translated)
    }

}
