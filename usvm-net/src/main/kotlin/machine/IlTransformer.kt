package org.usvm.machine

import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UAddressSort
import org.usvm.UContext
import org.usvm.UExpr
import org.usvm.USort
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.memory.UReadOnlyMemory
import org.usvm.org.usvm.expressions.UnsafeComposer
import org.usvm.org.usvm.expressions.UnsafeTransformer
import org.usvm.org.usvm.expressions.UnsafeTranslator

interface IlTransformer : UnsafeTransformer<IlType, USizeSort> {
    fun <Sort: USort> transform(ref: IlManagedHeapRef<Sort>): UExpr<UAddressSort>
    fun <Sort: USort> transform(ref: IlManagedStackRef<Sort>): UExpr<UAddressSort>
    fun <Sort: USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort>
}

class IlComposer(ctx: UContext<USizeSort>, memory: UReadOnlyMemory<IlType>, ownership: MutabilityOwnership) :
    UnsafeComposer<IlType, USizeSort>(ctx, memory, ownership), IlTransformer {
    override fun <Sort : USort> transform(ref: IlManagedHeapRef<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort : USort> transform(ref: IlManagedStackRef<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort: USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }
}

class IlTranslator(ctx: UContext<USizeSort>) : IlTransformer, UnsafeTranslator<IlType, USizeSort>(ctx) {
    override fun <Sort : USort> transform(ref: IlManagedHeapRef<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort : USort> transform(ref: IlManagedStackRef<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Sort: USort> transform(ptr: IlPtr<Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }
}
