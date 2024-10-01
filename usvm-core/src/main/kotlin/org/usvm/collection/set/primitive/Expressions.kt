package org.usvm.collection.set.primitive

import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import org.usvm.*
import org.usvm.collection.set.USymbolicSetElement
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.regions.Region

class UAllocatedSetReading<SetType, ElementSort : USort, Reg : Region<Reg>> internal constructor(
    ctx: UContext<*>,
    collection: UAllocatedSet<SetType, ElementSort, Reg>,
    val element: UExpr<ElementSort>,
) : UCollectionReading<UAllocatedSetId<SetType, ElementSort, Reg>, UExpr<ElementSort>, UBoolSort>(ctx, collection) {

    override fun accept(transformer: KTransformerBase): UBoolExpr {
        require(transformer is UTransformer<*, *>) { "Expected a UTransformer, but got: $transformer" }
        return transformer.asTypedTransformer<SetType, USort>().transform(this)
    }

    override fun readingConflict(composer: ConflictsComposer<*, *>): MutabilityOwnership =
        composer.asTypedComposer<SetType, USort>().getReadingConflict(this)

    override fun internEquals(other: Any): Boolean =
        structurallyEqual(
            other,
            { collection },
            { element },
        )

    override fun internHashCode(): Int = hash(collection, element)

    override fun print(printer: ExpressionPrinter) {
        printer.append("(")
        printer.append(element)
        printer.append(" in ")
        printer.append(collection.toString())
        printer.append(")")
    }
}

class UInputSetReading<SetType, ElementSort : USort, Reg : Region<Reg>> internal constructor(
    ctx: UContext<*>,
    collection: UInputSet<SetType, ElementSort, Reg>,
    val address: UHeapRef,
    val element: UExpr<ElementSort>
) : UCollectionReading<UInputSetId<SetType, ElementSort, Reg>, USymbolicSetElement<ElementSort>, UBoolSort>(
    ctx,
    collection
) {
    init {
        require(address !is UNullRef)
    }

    override fun accept(transformer: KTransformerBase): UBoolExpr {
        require(transformer is UTransformer<*, *>) { "Expected a UTransformer, but got: $transformer" }
        return transformer.asTypedTransformer<SetType, USort>().transform(this)
    }

    override fun readingConflict(composer: ConflictsComposer<*, *>): MutabilityOwnership =
        composer.asTypedComposer<SetType, USort>().getReadingConflict(this)

    override fun internEquals(other: Any): Boolean =
        structurallyEqual(
            other,
            { collection },
            { address },
            { element },
        )

    override fun internHashCode(): Int = hash(collection, address, element)

    override fun print(printer: ExpressionPrinter) {
        printer.append("(")
        printer.append(element)
        printer.append(" in ")
        printer.append(collection.toString())
        printer.append("[")
        printer.append(address)
        printer.append("]")
        printer.append(")")
    }
}
