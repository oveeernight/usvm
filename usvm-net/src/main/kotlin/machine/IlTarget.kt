package org.usvm.machine

import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.targets.UTarget

class IlTarget(location: IlStmt? = null) : UTarget<IlStmt, IlTarget>(location)
