package org.usvm.machine.state

import com.jetbrains.rd.util.string.print
import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.expr.KExpr
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.transformer.KTransformerBase
import io.ksmt.utils.asExpr
import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlField
import org.usvm.*
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.machine.*
import org.usvm.memory.ULValue
import org.usvm.memory.UMemoryRegion
import org.usvm.memory.UMemoryRegionId
import org.usvm.memory.USymbolicCollectionId

fun UExpr<*>.toStruct() : IlStruct =
    when (this) {
        is IlStruct -> this
        is UCollectionReading<*, *, *> -> with(sort.ilctx) {
            val structSort = sort as StructSort
            val structType = structSort.structType
            val fields = structType.fields.map { f ->
                f to StructFieldReading(this@with, this@toStruct, f, typeToSort(f.fieldType))
            }.fold(persistentHashMapOf<IlField, UExpr<out USort>>()) { fields, (f, v) ->
                fields.put(f, v, defaultOwnership)
            }
            mkStruct(structType, fields)
        }
        else -> error("Unexpected struct expr $this")
    }

class StructFieldReading<Key, Sort : USort, CollectionId : USymbolicCollectionId<Key, *, CollectionId>>(
    ctx: UContext<*>,
    val base: UCollectionReading<CollectionId, Key, *>,
    val field: IlField,
    override val sort: Sort
) : USymbol<Sort>(ctx) {
    override fun internEquals(other: Any): Boolean = structurallyEqual(other,
        { base },
        { field },
        { sort }
    )

    override fun accept(transformer: KTransformerBase): KExpr<Sort> {
        require(transformer is IlTransformer) { "Expected IlTransformer, but got $transformer" }
        return transformer.transform(this)
    }

    override fun internHashCode(): Int = hash(base, field, sort)

    override fun print(printer: ExpressionPrinter) {
        base.print(printer)
        printer.append(".${field.name}")
    }
}

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
        val reading = structRegion.read(structKey)
        val struct = reading.toStruct()
        return struct.fields[field].cast()
    }

    override fun write(
        key: StructFieldLValue<Key, Sort>,
        value: UExpr<Sort>,
        guard: UBoolExpr,
        ownership: MutabilityOwnership
    ): UMemoryRegion<StructFieldLValue<Key, Sort>, Sort> {
        val reading = structRegion.read(structKey)
        val struct = reading.toStruct()
        val updated = struct.writeField(field, value, ownership)
        val updatedStructRegion = structRegion.write(structKey, updated.cast(), guard, ownership)
        return StructsMemoryRegion(sort, updatedStructRegion, structKey, field)
    }
}
