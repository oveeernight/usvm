package org.usvm.machine.interpreter

import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType

fun IlType.findMethod(method: IlMethod) : IlMethod {
    val typeMethods = methods
    // TODO method name is not enough!!!
    typeMethods.find { it.name == method.name }?.let { return it }
    val declaringType = baseType!!
    return declaringType.findMethod(method)
}
