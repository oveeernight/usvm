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
    fun <Key, Sort: USort> transform(ref: IlManagedRef<Key, Sort>): UExpr<UAddressSort>
    fun <Key, Sort: USort> transform(ptr: IlPtr<Key, Sort>): UExpr<UAddressSort>
}

class IlComposer(ctx: UContext<USizeSort>, memory: UReadOnlyMemory<IlType>, ownership: MutabilityOwnership) :
    UnsafeComposer<IlType, USizeSort>(ctx, memory, ownership), IlTransformer {
    override fun <Key, Sort : USort> transform(ref: IlManagedRef<Key, Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Key, Sort : USort> transform(ptr: IlPtr<Key, Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }
}

class IlTranslator(ctx: UContext<USizeSort>) : IlTransformer, UnsafeTranslator<IlType, USizeSort>(ctx) {
    override fun <Key, Sort : USort> transform(ref: IlManagedRef<Key, Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }

    override fun <Key, Sort : USort> transform(ptr: IlPtr<Key, Sort>): UExpr<UAddressSort> {
        TODO("Not yet implemented")
    }
}
