package org.usvm.machine

import org.jacodb.api.net.ilinstances.IlType
import org.usvm.UContext
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.memory.UReadOnlyMemory
import org.usvm.org.usvm.expressions.UnsafeComposer
import org.usvm.org.usvm.expressions.UnsafeTransformer
import org.usvm.org.usvm.expressions.UnsafeTranslator

interface IlTransformer : UnsafeTransformer<IlType, USizeSort> {}

class IlComposer(ctx: UContext<USizeSort>, memory: UReadOnlyMemory<IlType>, ownership: MutabilityOwnership) :
    UnsafeComposer<IlType, USizeSort>(ctx, memory, ownership), IlTransformer {}

class IlTranslator(ctx: UContext<USizeSort>) : IlTransformer, UnsafeTranslator<IlType, USizeSort>(ctx) {}
