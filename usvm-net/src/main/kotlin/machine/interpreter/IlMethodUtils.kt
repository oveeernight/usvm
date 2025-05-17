package org.usvm.machine.interpreter

import org.jacodb.api.net.ilinstances.*
import org.jacodb.api.net.ilinstances.impl.IlMethodImpl

fun IlType.findMethod(method: IlMethod) : IlMethod {
    val typeMethods = methods
    // TODO method name is not enough!!!
    typeMethods.find { it.name == method.name }?.let { return it }
    val declaringType = baseType!!
    return declaringType.findMethod(method)
}

fun IlMethod.mapLocalToRegisterStackIdx(local: IlLocal): Int =
    when (local) {
        is IlArgument -> local.index

        is IlLocalVar -> parameters.size + local.index

        is IlTempVar -> parameters.size + (this as IlMethodImpl).locals.size + local.index
        is IlErrVar -> parameters.size + (this as IlMethodImpl).locals.size + temps.size + local.index
        else -> error("mapLocalToRegisterStackIdx: unexpected local $local")
    }

fun IlMethod.typeOfRegister(reg: Int) : IlType {
    this as IlMethodImpl
    val paramsCount = parameters.size
    val localsCount = locals.size
    val tempsCount = temps.size
    val errsCount = errs.size
    return when {
        reg < paramsCount -> parameters[reg].type
        reg < paramsCount + localsCount -> locals[reg - paramsCount].type
        reg < paramsCount + localsCount + tempsCount -> temps[reg - paramsCount - localsCount].type
        reg < paramsCount + localsCount + tempsCount + errsCount -> errs[reg - paramsCount - localsCount - tempsCount].type
        else -> error("Unexpected reg $reg")
    }
}
