package org.usvm.collection.map.length

import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import org.usvm.*
import org.usvm.collections.immutable.internal.MutabilityOwnership

class UInputMapLengthReading<MapType, USizeSort : USort> internal constructor(
    ctx: UContext<USizeSort>,
    collection: UInputMapLengthCollection<MapType, USizeSort>,
    val address: UHeapRef,
) : UCollectionReading<UInputMapLengthId<MapType, USizeSort>, UHeapRef, USizeSort>(ctx, collection) {
    init {
        require(address !is UNullRef)
    }

    override fun accept(transformer: KTransformerBase): UExpr<USizeSort> {
        require(transformer is UTransformer<*, *>) { "Expected a UTransformer, but got: $transformer" }
        return transformer.asTypedTransformer<MapType, USizeSort>().transform(this)
    }

    override fun readingConflict(composer: ConflictsComposer<*, *>): MutabilityOwnership =
        composer.asTypedComposer<MapType, USizeSort>().getReadingConflict(this)

    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { collection }, { address })

    override fun internHashCode(): Int = hash(collection, address)

    override fun print(printer: ExpressionPrinter) {
        printer.append(collection.toString())
        printer.append("[")
        printer.append(address)
        printer.append("]")
    }
}
