package org.usvm.machine.state

import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlStmt
import org.usvm.UCallStack
import org.usvm.UCallStackFrame

/*
    In .NET, mechanism of exception handling in first bypass may execute filters on
    arbitrary frame, without popping any stack frames. So, we must preserve observing stack frame.
    Filters may execute other functions, and new stack frames can be added, and we must
    backtrack previously observed frame to be able to return in filter stack frame.
 */
class IlCallStack(
    stack: ArrayDeque<UCallStackFrame<IlMethod, IlStmt>>,
    private val retSiteFramesStack: ArrayDeque<Int> = ArrayDeque(),
    ): UCallStack<IlMethod, IlStmt>(stack) {
    constructor() : this(ArrayDeque(), ArrayDeque())

    constructor(method: IlMethod): this(
        ArrayDeque<UCallStackFrame<IlMethod, IlStmt>>().apply {
            val firstFrame = UCallStackFrame(method, null as IlStmt?)
            add(firstFrame)
        },
        ArrayDeque<Int>().apply { add(-1) }
    )

    fun push(method: IlMethod, returnSite: IlStmt?, retSiteFrameIndex: Int) {
        stack.add(UCallStackFrame(method, returnSite))
        retSiteFramesStack.add(retSiteFrameIndex)
    }

    fun popWithRetSiteFrameIdx(): Pair<IlStmt?, Int> {
        val retSite = stack.removeLast().returnSite
        val retSiteFrameIdx = retSiteFramesStack.removeLast()
        return retSite to retSiteFrameIdx
    }

    override fun clone(): IlCallStack {
        val newStack = ArrayDeque<UCallStackFrame<IlMethod, IlStmt>>()
        newStack.addAll(stack)
        val newRetSiteFramesStack = ArrayDeque<Int>()
        newRetSiteFramesStack.addAll(retSiteFramesStack)
        return IlCallStack(newStack, newRetSiteFramesStack)
    }
}
