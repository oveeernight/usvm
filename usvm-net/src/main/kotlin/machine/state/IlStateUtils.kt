package org.usvm.machine.state

import io.ksmt.utils.asExpr
import org.jacodb.api.net.ilinstances.IlLocal
import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlMethodImpl
import org.jacodb.api.net.ilinstances.impl.IlStructType
import org.usvm.*
import org.usvm.api.allocateConcreteRef
import org.usvm.collection.field.UFieldLValue
import org.usvm.machine.interpreter.*
import org.usvm.machine.write

val IlState.lastStackTraceFrame: UStackTraceFrame<IlMethod, IlStmt>
    get() = callStack.stackTrace(currentStatement).last()

fun IlState.newStmt(stmt: IlStmt) {
    pathNode += stmt
}

fun IlState.returnValue(value: UExpr<out USort>) {
    val method = callStack.lastMethod()
    val res = IlMethodResult.Success(value, method)
    methodResult = res
    val (returnSite, retSiteFrameIdx) = callStack.popWithRetSiteFrameIdx()
    returnSite?.let {
        memory.stack.pop()
        memory.stack.observingFrame = retSiteFrameIdx
        newStmt(it)
    }
}

//fun IlMethod.toLocalIdx(idx: Int): Int = if (this.isStatic) idx else idx + 1
//fun IlMethod.paramsWithThisCount() : Int = toLocalIdx(parameters.size)
fun IlMethod.localsCount() : Int {
    this as IlMethodImpl
    return locals.size + temps.size + errs.size
}

fun IlState.throwException(type: IlType, frame: UStackTraceFrame<IlMethod, IlStmt>) {
    val ref = ctx.allocateConcreteRef()
    memory.types.allocate(ref.address, type)
    val exception = IlMethodResult.Exception(ref, type, frame.method, frame.instruction)
    methodResult = exception
    exceptionsStack.add(UnhandledExceptionEntry(exception))
}

fun IlState.terminate() {
    while (callStack.isNotEmpty()) {
        callStack.pop()
        if (callStack.isNotEmpty()) {
            memory.stack.pop()
        }
    }
}



// it is expected to not kill application
fun IlState.dropFrames(count: Int) {
    var i = count
    while (i > 0) {
        val (_, retSiteFrameIdx) = callStack.popWithRetSiteFrameIdx()
        memory.stack.observingFrame = retSiteFrameIdx
        memory.stack.pop()
        i--;
    }
}

fun IlState.dropFramesAfterIndex(lastIdx: Int) {
    val count = callStack.lastIndex - lastIdx
    dropFrames(count)
}

fun IlState.insertConcreteCallStmt(method: IlMethod, args: List<UExpr<out USort>>) =
    newStmt(IlConcreteCallStmt(method, args, currentStatement))

fun IlState.insertVirtualCallStmt(method: IlMethod, args: List<UExpr<out USort>>) =
    newStmt(IlVirtualCallStmt(method, args, currentStatement))

fun IlState.callMethod(method: IlMethod, args: List<UExpr<out USort>>, returnSite: IlStmt) {
    if (method.returnType == ctx.voidType && method.instList.isEmpty()) {
        methodResult = IlMethodResult.Success(ctx.void, method)
        newStmt(returnSite)
        return
    }
    val observingFrameIdx = memory.stack.observingFrame
    callStack.push(method, returnSite, observingFrameIdx)
    memory.stack.push(args.toTypedArray(), method.localsCount())
    // here we need to initialize local variables for structs to default values, because
    // structs are initialized via ctor call, without new instruction
    newStmt(method.instList.first())
}

// TODO rec initialize
fun IlState.initializeStructLocals(method: IlMethod, localsMapper: (IlMethod, IlLocal) -> Int) {
    method as IlMethodImpl
    val locals = method.locals
    val frameIdx = callStack.size - 1
    for (local in locals) {
        if (local.type is IlStructType) {
            val regStackIdx = localsMapper(method, local)
            val key = IlRegisterStackLValue(ctx.addressSort, frameIdx, regStackIdx)
            val ref = memory.allocConcrete(local.type)
            memory.write(key, ref)
        }
    }
}

fun IlState.typeIsInitialized(ilType: IlType): Boolean {
    val staticFieldsAreInitialized = IlStaticFieldLValue(ctx.staticFieldsInitializedFlag, ctx.boolSort)
    return memory.read(staticFieldsAreInitialized).isTrue
}

internal fun IlState.copyStruct(structRef: UHeapRef, type: IlStructType): UHeapRef {
    val copyRef = memory.allocConcrete(type)
    type.fields.forEach { f ->
        val fieldType = f.fieldType
        val fieldSort = ctx.typeToSort(fieldType)
        val fieldValue = UFieldLValue(fieldSort, structRef, f).let {
            val value = memory.read(it)
            if (fieldType is IlStructType) {
                copyStruct(value.asExpr(ctx.addressSort), fieldType)
            } else value
        }
        UFieldLValue(fieldSort, copyRef, f).let { memory.write(it, fieldValue) }
    }
    return copyRef
}

fun IlState.skipMethodInvokeWithValue(call: MethodCall, result: UExpr<out USort>) {
    methodResult = IlMethodResult.Success(result, call.method)
    newStmt(call.returnSite)
}
