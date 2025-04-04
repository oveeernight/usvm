package org.usvm.machine.state

import org.usvm.*
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.memory.*

object IlRegisterStackId : UMemoryRegionId<URegisterStackLValue<*>, USort> {
    override val sort: USort
        get() = error("Register stack has no sort")
    override fun emptyRegion(): UMemoryRegion<URegisterStackLValue<*>, USort> = IlRegistersStack()
}

class IlRegisterStackLValue<Sort: USort>(
    override val sort: Sort,
    val frameIdx: Int,
    val regIdx: Int
): URegisterStackLValue<Sort>(sort, regIdx)  {
    override val memoryRegionId: UMemoryRegionId<URegisterStackLValue<*>, USort>
        get() = IlRegisterStackId

    override val key: URegisterStackLValue<Sort> = this

}

class IlRegistersStack(frames: MutableList<Array<UExpr<out USort>?>> = mutableListOf()) : URegistersStack(frames) {
    private fun writeFrame(frameIndex: Int, regIndex: Int, value: UExpr<out USort>) {
        frames[frameIndex][regIndex] = value
    }

    private fun <Sort: USort> readFrame(frameIndex: Int, regIndex: Int, sort: Sort) : UExpr<Sort> {
        return if (frames.size > 0) {
            frames[frameIndex].read(regIndex, sort)
        } else { // getInitialStateCase
            sort.uctx.mkRegisterReading(regIndex, sort)
        }
    }

    override fun read(key: URegisterStackLValue<*>): UExpr<USort> {
        require(key is IlRegisterStackLValue<*>)
        return readFrame(key.frameIdx, key.regIdx, key.sort)
    }

    override fun write(
        key: URegisterStackLValue<*>,
        value: UExpr<USort>,
        guard: UBoolExpr,
        ownership: MutabilityOwnership
    ): UMemoryRegion<URegisterStackLValue<*>, USort> {
        require(key is IlRegisterStackLValue<*>)
        check(guard.isTrue) { "Guarded writes are not supported for register" }
        writeFrame(key.frameIdx, key.regIdx, value)
        return this
    }

    override fun clone(): URegistersStack {
        val newStack = ArrayDeque(frames.map { it.clone() })
        return URegistersStack(newStack)
    }
}
