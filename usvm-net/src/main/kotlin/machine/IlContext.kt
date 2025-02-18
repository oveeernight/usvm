package org.usvm.machine

import org.usvm.UBv32Sort
import org.usvm.UContext

typealias USizeSort = UBv32Sort

class IlContext(components: IlComponents) : UContext<USizeSort>(components)
