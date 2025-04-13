package org.usvm.machine

import io.ksmt.utils.cast
import org.jacodb.api.net.ilinstances.IlField
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.*
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.collections.immutable.persistentHashMapOf
import org.usvm.machine.state.StructFieldReading
import org.usvm.machine.state.toStruct
import org.usvm.memory.GuardedExpr
import org.usvm.memory.UReadOnlyMemory
import org.usvm.memory.USymbolicCollectionId
import org.usvm.memory.with
import org.usvm.org.usvm.expressions.UnsafeComposer
import org.usvm.org.usvm.expressions.UnsafeTransformer
import org.usvm.org.usvm.expressions.UnsafeTranslator
import kotlin.concurrent.thread

interface IlTransformer : UnsafeTransformer<IlType, USizeSort> {
    fun transform(struct: IlStruct) : IlStruct
    fun <Sort: USort> transform(ref: IlManagedRef<Sort>): UExpr<UAddressSort>
    fun <Sort: USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort>
    fun <Key, CollectionId : USymbolicCollectionId<Key, *, CollectionId>, Sort : USort> transform(
        structFieldReading: StructFieldReading<Key, Sort, CollectionId>
    ): UExpr<Sort>
}

class IlComposer(ctx: UContext<USizeSort>, memory: UReadOnlyMemory<IlType>, ownership: MutabilityOwnership) :
    UnsafeComposer<IlType, USizeSort>(ctx, memory, ownership), IlTransformer {
    override fun transform(struct: IlStruct): IlStruct {
        val composedFields = struct.fields.fold(persistentHashMapOf<IlField, UExpr<out USort>>()) { fields, (f, v) ->
            val composedValue = compose(v)
            fields.put(f, composedValue, memory.ownership)
        }
        return IlStruct(ctx as IlContext, struct.sort, struct.type, composedFields)
    }

    override fun <Sort : USort> transform(ref: IlManagedRef<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort: USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Key, CollectionId : USymbolicCollectionId<Key, *, CollectionId>, Sort : USort> transform(
        structFieldReading: StructFieldReading<Key, Sort, CollectionId>
    ): UExpr<Sort> {
        val composedReading = compose(structFieldReading.base)
        val struct = composedReading.toStruct()
        return compose(struct.fields[structFieldReading.field].cast())
    }
}

class IlTranslator(ctx: UContext<USizeSort>) : IlTransformer, UnsafeTranslator<IlType, USizeSort>(ctx) {
    override fun transform(struct: IlStruct): IlStruct {
        val transformedFields = struct.fields.fold(persistentHashMapOf<IlField, UExpr<out USort>>()) { fields, (f, v) ->
            val transformedValue = v.accept(this)
            fields.put(f, transformedValue, ctx.defaultOwnership)
        }
        return IlStruct(ctx as IlContext, struct.sort, struct.type, transformedFields)
    }

    override fun <Sort : USort> transform(ref: IlManagedRef<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort : USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Key, CollectionId : USymbolicCollectionId<Key, *, CollectionId>, Sort : USort> transform(
        structFieldReading: StructFieldReading<Key, Sort, CollectionId>
    ): UExpr<Sort> {
        val transformedReading = structFieldReading.base.accept(this)
        return structFieldProjection(transformedReading, structFieldReading.field)
    }

    private fun <Sort : USort> structFieldProjection(expr: UExpr<out USort>, field: IlField): UExpr<Sort> = with(ctx) {
        val projected = mutableListOf<UExpr<Sort>>()
        val queue = mutableListOf<Pair<TraverseState, UExpr<out USort>>>()
        queue.add(TraverseState.LEFT to expr)
        while (queue.isNotEmpty()) {
            val (state, e) = queue.removeLast()
            when (e) {
                is IlStruct -> projected.add(e.readField(field))
                is UIteExpr<*> -> {
                    when (state) {
                        TraverseState.LEFT -> {
                            queue.add(TraverseState.RIGHT to e)
                            queue.add(TraverseState.LEFT to e.trueBranch)
                        }

                        TraverseState.RIGHT -> {
                            queue.add(TraverseState.DONE to e)
                            queue.add(TraverseState.LEFT to e.falseBranch)
                        }

                        TraverseState.DONE -> {
                            val rhs = projected.removeLast()
                            val lhs = projected.removeLast()
                            val result = ctx.mkIte(e.condition, rhs, lhs)
                            projected.add(result)
                        }
                    }
                }

                else -> error("Unexpected struct $e")
            }
        }
        projected.single()
    }

    private enum class TraverseState {
        LEFT,
        RIGHT,
        DONE
    }
}
