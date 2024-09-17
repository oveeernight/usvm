/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.usvm.collections.immutable.internal

/**
 * The mutability ownership token of a block of code without forks.
 *
 * Used to mark persistent data structures, that are owned by a continous block of code and can be mutated by it.
 */
open class MutabilityOwnership
