package org.usvm

import org.jacodb.api.net.generated.models.IlPointerTypeDto
import org.jacodb.api.net.ilinstances.IlType
import org.jacodb.api.net.ilinstances.impl.IlPointerType

val IlPointerType.targetType: IlType get() {
    val field = this::class.java.getField("dto")
    field.isAccessible = true

    val res = field.get(this) as IlPointerTypeDto
    field.isAccessible = false

    return publication.findIlTypeOrNull(res.targetType)!!
}
