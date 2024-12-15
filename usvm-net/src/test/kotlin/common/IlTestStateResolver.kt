package common

import io.ksmt.utils.asExpr
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlArrayType
import org.jacodb.api.net.ilinstances.impl.IlPrimitiveType
import org.jacodb.api.net.ilinstances.impl.IlReferenceType
import org.usvm.*
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.array.length.UArrayLengthLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.IlContext
import org.usvm.machine.state.toLocalIdx
import org.usvm.machine.tryBool
import org.usvm.machine.tryInt16
import org.usvm.machine.tryInt32
import org.usvm.machine.tryInt64
import org.usvm.machine.tryInt8
import org.usvm.machine.tryChar
import org.usvm.machine.tryDouble
import org.usvm.machine.tryFloat
import org.usvm.memory.ULValue
import org.usvm.memory.UReadOnlyMemory
import org.usvm.memory.URegisterStackLValue
import org.usvm.model.UModelBase
import org.usvm.types.single

abstract class IlTestStateResolver<T>(
    val ctx: IlContext,
    val method: IlMethod,
    val model: UModelBase<IlType>,
    val stateMemory: UReadOnlyMemory<IlType>
) {
    abstract val decoderApi: DecoderApi<T>

    private val cache = mutableMapOf<UConcreteHeapAddress, T>()

    private var resolveMode: ResolveMode = ResolveMode.MODEL

    private val memory : UReadOnlyMemory<IlType> get() =
        when (resolveMode) {
            ResolveMode.MODEL -> model
            ResolveMode.STATE_MEMORY -> stateMemory
        }

    fun <Sort: USort> resolve(expr: UExpr<Sort>, type: IlType): T {
        return when {
            ctx.isPrimitiveType(type) -> resolvePrimitive(expr, type)
            else -> resolveReference(expr.asExpr(ctx.addressSort), type)
//            else -> error("Unexpected type ${type.name}")
        }
    }

    private fun resolveLValue(key: ULValue<*, *>, type: IlType): T {
        val expr = memory.read(key)
        return resolve(expr, type)
    }

    protected fun resolveThis(): T {
        val declaringType = method.declaringType
        return if (method.isStatic) {
            decoderApi.createNullConst(declaringType)
        } else {
            val stackKey = URegisterStackLValue(ctx.typeToSort(declaringType), 0)
            resolveLValue(stackKey, declaringType)
        }
    }

    protected fun resolveArgs(): List<T> =
        method.parameters.mapIndexed { i, param ->
            URegisterStackLValue(ctx.typeToSort(param.type), i).let { resolveLValue(it, param.type) }
        }

    private fun <Sort: USort> resolvePrimitive(expr: UExpr<Sort>, type: IlType): T = with(ctx) {
        when (type) {
            boolType -> decoderApi.createBoolConst(resolveBool(expr))
            charType -> decoderApi.createCharConst(resolveChar(expr))
//            int8Type -> decoderApi.createInt8Const(resolveInt8(expr))
            int16Type -> decoderApi.createInt16Const(resolveInt16(expr))
            int32Type -> decoderApi.createInt32Const(resolveInt32(expr))
            int64Type -> decoderApi.createInt64Const(resolveInt64(expr))
            floatType -> decoderApi.createFloatConst(resolveFloat(expr))
            doubleType -> decoderApi.createDoubleConst(resolveDouble(expr))
            else -> error("Unexpected primitive ${type.name}")
        }
    }

    private fun <Sort: USort> resolveBool(expr: UExpr<Sort>) = evalExpr(expr).tryBool() ?: false
    private fun <Sort: USort> resolveInt8(expr: UExpr<Sort>) = evalExpr(expr).tryInt8() ?: 0
    private fun <Sort: USort> resolveInt16(expr: UExpr<Sort>) = evalExpr(expr).tryInt16() ?: 0
    private fun <Sort: USort> resolveInt32(expr: UExpr<Sort>) = evalExpr(expr).tryInt32() ?: 0
    private fun <Sort: USort> resolveInt64(expr: UExpr<Sort>) = evalExpr(expr).tryInt64() ?: 0
    private fun <Sort: USort> resolveChar(expr: UExpr<Sort>) = evalExpr(expr).tryChar() ?: '\u0000'
    private fun <Sort: USort> resolveFloat(expr: UExpr<Sort>) = evalExpr(expr).tryFloat() ?: 0f
    private fun <Sort: USort> resolveDouble(expr: UExpr<Sort>) = evalExpr(expr).tryDouble() ?: 0.0

    private fun resolveReference(heapRef: UHeapRef, type: IlType): T {
        val evaledRef = model.eval(heapRef) as UConcreteHeapRef

        if (evaledRef.address == NULL_ADDRESS) {
            return decoderApi.createNullConst(type)
        }

//        val types = if (evaledRef.isStatic) {
//            memory.types.getTypeStream(evaledRef)
//        } else {
//            assert(evaledRef.address <= INITIAL_INPUT_ADDRESS)
//            model.types.getTypeStream(evaledRef)
//        }.filterBySupertype(type)

        val runtimeType = type

        return resolveCyclic(evaledRef, runtimeType) {
            when (runtimeType) {
                is IlArrayType -> resolveArray(heapRef, evaledRef, runtimeType)
                ctx.stringType -> resolveString(heapRef, evaledRef)
                else -> resolveObject(heapRef, evaledRef, runtimeType)
            }
        }
    }

    private fun resolveCyclic(ref: UConcreteHeapRef, type: IlType, resolve: () -> T): T {
        if (cache.containsKey(ref.address)) return decoderApi.createCyclicReference(type, ref.address)
        return resolve()
    }

    private fun resolveArray(heapRef: UHeapRef, evaledRef: UConcreteHeapRef, type: IlArrayType): T {
        val memory = memoryToRead(evaledRef)
        val descriptor = ctx.arrayDescriptorOf(type)
        val elemType = type.elementType
        val sort = ctx.typeToSort(elemType)
        val lengthKey = UArrayLengthLValue(heapRef, descriptor, sort)
        val length = memory.read(lengthKey).tryInt32() ?: error("array $evaledRef length is not integer")
        val array = decoderApi.createArray(elemType, length, evaledRef.address)

        for (index in 0 until length) {
            val indexKey = UArrayIndexLValue(sort, heapRef, ctx.mkBv(index), descriptor)
            val resolved = resolve(memory.read(indexKey), elemType)
            decoderApi.setArrayIndex(array, index, resolved)
        }

        cache[evaledRef.address] = array
        return array
    }

    private fun resolveString(heapRef: UHeapRef, evaledRef: UConcreteHeapRef): T {
        val descriptor = ctx.charType
        val elemType = ctx.charType
        val elemSort = ctx.typeToSort(elemType)
        val lengthKey = UArrayLengthLValue(heapRef, descriptor, elemSort)
        val length = memory.read(lengthKey).tryInt32() ?: error("string $evaledRef length is not integer")
        val content = CharArray(length)

        for (index in 0 until length) {
            val indexKey = UArrayIndexLValue(elemSort, heapRef, ctx.mkBv(index), descriptor)
            val char = resolve(memory.read(indexKey), elemType) as? Char ?: error("string $evaledRef content is not char array")
            content[index] = char
        }

        val string = decoderApi.createStringConst(content.concatToString())
        cache[evaledRef.address] = string
        return string
    }

    private fun resolveObject(heapRef: UHeapRef, evaledRef: UConcreteHeapRef, type: IlType): T {
        val obj = decoderApi.createObject(type, evaledRef.address)
        for (field in type.fields) {
            val memory = memoryToRead(evaledRef)
            val fieldSort = ctx.typeToSort(field.fieldType)
            val fieldKey = UFieldLValue(fieldSort, heapRef, field)
            val resolvedValue = resolve(memory.read(fieldKey), field.fieldType)
            decoderApi.setObjectField(obj, field, resolvedValue)
        }
        cache[evaledRef.address] = obj
        return obj
    }

    private fun <Sort: USort> evalExpr(expr: UExpr<Sort>): UExpr<Sort> {
        return model.eval(expr)
    }

    private fun memoryToRead(evaledRef: UConcreteHeapRef) =
        if (evaledRef.address <= INITIAL_STATIC_ADDRESS) memory else model

    private enum class ResolveMode { MODEL, STATE_MEMORY }
}
