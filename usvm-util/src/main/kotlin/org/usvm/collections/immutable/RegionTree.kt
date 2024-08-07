package org.usvm.collections.immutable

import org.usvm.collections.immutable.implementations.immutableMap.UPersistentHashMap
import org.usvm.collections.immutable.internal.EndOfChain
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.regions.Region

internal class LinkedValue<V>(val value: V, val previous: Any?, val next: Any?) {
    /** Constructs LinkedValue for a new single entry */
    constructor(value: V) : this(value, EndOfChain, EndOfChain)
    /** Constructs LinkedValue for a new last entry */
    constructor(value: V, previous: Any?) : this(value, previous, EndOfChain)

    fun withValue(newValue: V) = LinkedValue(newValue, previous, next)
    fun withPrevious(newPrevious: Any?) = LinkedValue(value, newPrevious, next)
    fun withNext(newNext: Any?) = LinkedValue(value, previous, newNext)

    val hasNext get() = next !== EndOfChain
    val hasPrevious get() = previous !== EndOfChain
}

class RegionTree<Reg, Value> private constructor(
    private var entries: UPersistentHashMap<Reg, LinkedValue<Pair<Value, RegionTree<Reg, Value>>>>,
    private var firstKey: Any?,
    private var lastKey: Any?
) : Iterable<Pair<Value, Reg>> where Reg : Region<Reg> {
    constructor() : this(persistentHashMapOf(), EndOfChain, EndOfChain)

    fun clone() = RegionTree(entries, firstKey, lastKey)

    val isEmpty: Boolean get() = entries.isEmpty()

    /**
     * Splits the region tree into two trees: completely covered by the [region] and disjoint with it.
     *
     * [filterPredicate] is a predicate suitable to filter out particular nodes if their `value` don't satisfy it.
     * Examples:
     * * `{ true }` doesn't filter anything
     * * `{ it != value }` writes into a tree and restrict for it to contain non-unique values.
     * @see localize
     */
    private fun splitRecursively(
        region: Reg,
        filterPredicate: (Value) -> Boolean,
    ): RecursiveSplitResult {
        if (isEmpty) {
            return RecursiveSplitResult(completelyCoveredRegionTree = this, disjointRegionTree = this)
        }

        val linkedEntry = entries[region]

        // If we have precisely such region in the tree, then all its siblings are disjoint by invariant (1).
        // So just return a `Pair(node storing the region, rest of its siblings)`
        if (linkedEntry != null) {
            val (value, tree) = linkedEntry.value
            // IMPORTANT: usage of linked versions of maps is mandatory here, since
            // it is required for correct order of values returned by `iterator.next()` method
            val completelyCoveredRegionTree = if (filterPredicate(value)) {
                if (entries.singleOrNull() != null) {
                    this
                } else {
                    val insideTree = RegionTree<Reg, Value>()
                    insideTree.put(region, value to tree)
                    insideTree
                }
            } else {
                // fixme: here we might have a deep recursion, maybe we should rewrite it
                tree.splitRecursively(region, filterPredicate).completelyCoveredRegionTree
            }

            val disjointRegionTree = clone()
            disjointRegionTree.remove(region, ownership = null)

            return RecursiveSplitResult(completelyCoveredRegionTree, disjointRegionTree)
        }

        // Such region doesn't exist. Do it slow way: iterate all the entries, group them into:
        // (1) included by the [region],
        // (2) disjoint with the [region],
        // (3) partially intersected with the [region].

        // IMPORTANT: usage of linked versions of maps is mandatory here, since
        // it is required for correct order of values returned by `iterator.next()` method
        // We have to support the following order: assume that we had entries [e0, e1, e2, e3, e4]
        // and a write operation into a region R that is a subregion of `e1` and `e3`, and covers e2 completely.
        // The correct order of the result is:
        // included = [R ∩ e1, e2, R ∩ e3], disjoint = [e0, e1\R, e3\R, e4]
        // That allows us to move recently updates region to the right side of the `entries` map and
        // leave the oldest ones in the left part of it.
        val includedTree = RegionTree<Reg, Value>()
        val disjointTree = RegionTree<Reg, Value>()

        fun RegionTree<Reg, Value>.addWithFilter(
            nodeRegion: Reg,
            valueWithRegionTree: Pair<Value, RegionTree<Reg, Value>>,
            filterPredicate: (Value) -> Boolean,
        ) {
            val (value, childRegionTree) = valueWithRegionTree
            if (filterPredicate(value)) {
                put(nodeRegion, valueWithRegionTree)
            } else {
                putAll(childRegionTree)
            }
        }

        rootEntriesIterator().forEach { (nodeRegion, valueWithRegionTree) ->
            when (region.compare(nodeRegion)) {
                Region.ComparisonResult.INCLUDES -> includedTree.addWithFilter(nodeRegion, valueWithRegionTree, filterPredicate)

                Region.ComparisonResult.DISJOINT -> disjointTree.addWithFilter(nodeRegion, valueWithRegionTree, filterPredicate)
                // For nodes with intersection, repeat process recursively.
                Region.ComparisonResult.INTERSECTS -> {
                    val (value, childRegionTree) = valueWithRegionTree
                    val (splitIncluded, splitDisjoint) = childRegionTree.splitRecursively(region, filterPredicate)

                    val includedReg = nodeRegion.intersect(region)
                    val disjointReg = nodeRegion.subtract(region)

                    includedTree.addWithFilter(includedReg, value to splitIncluded, filterPredicate)
                    disjointTree.addWithFilter(disjointReg, value to splitDisjoint, filterPredicate)
                }
            }
        }

        // IMPORTANT: usage of linked versions of maps is mandatory here, since
        // it is required for correct order of values returned by `iterator.next()` method

        return RecursiveSplitResult(includedTree, disjointTree)
    }

    /**
     * Returns a subtree completely included into the [region].
     *
     * [filterPredicate] is a predicate suitable to filter out particular nodes if their `value` don't satisfy it.
     * Examples:
     * * `{ true }` doesn't filter anything
     * * `{ it != value }` writes into a tree and restrict for it to contain non-unique values.
     *   Suitable for deduplication.
     *
     * r := some region
     * tree := {r -> 1}
     *             {r -> 2}
     *                 {r -> 3}
     * tree.localize(r) { it % 2 == 1) =
     *     // first will be filtered out
     *         {r -> 2}
     *            // third will be filtered out
     *
     * ```
     */
    fun localize(region: Reg, filterPredicate: (Value) -> Boolean = { true }): RegionTree<Reg, Value> =
        splitRecursively(region, filterPredicate).completelyCoveredRegionTree

    /**
     * Returns a subtree completely included into the [region] and disjoint with it.
     */
    fun split(
        region: Reg,
        filterPredicate: (Value) -> Boolean = { true },
    ): Pair<CompletelyCoveredRegionTree<Reg, Value>, DisjointRegionTree<Reg, Value>> {
        val splitResult = splitRecursively(region, filterPredicate)
        return splitResult.completelyCoveredRegionTree to splitResult.disjointRegionTree
    }

    /**
     * Places a Pair([region], [value]) into the tree, preserving its invariants.
     *
     * [filterPredicate] is a predicate suitable to filter out particular nodes if their `value` don't satisfy it.
     * Examples:
     * * `{ true }` doesn't filter anything
     * * `{ it != value }` writes into a tree and restrict for it to contain non-unique values.
     *   Suitable for deduplication.
     * @see localize
     */
    fun write(region: Reg, value: Value, filterPredicate: (Value) -> Boolean = { true }): RegionTree<Reg, Value> {
        var (included, disjoint) = splitRecursively(region, filterPredicate)
        // A new node for a tree we construct accordingly to the (2) invariant.
        val node = value to included

        if (included === disjoint) {
            disjoint = disjoint.clone()
        }
        // Construct entries accordingly to the (1) invariant.
        disjoint.put(region, node)

        return disjoint
    }

    private fun checkInvariantRecursively(parentRegion: Reg?): Boolean {
        // Invariant (2): all child regions are included into parent region
        val secondInvariant = parentRegion == null || entries.all { (reg, _) ->
            parentRegion.compare(reg) == Region.ComparisonResult.INCLUDES
        }

        val checkInvariantRecursively = {
            entries.all { (entryKey, value) ->
                // Invariant (1): all sibling regions are pairwise disjoint
                val firstInvariant = entries.all { other ->
                    val otherReg = other.key
                    otherReg === entryKey || entryKey.compare(otherReg) == Region.ComparisonResult.DISJOINT
                }

                firstInvariant && value.value.second.checkInvariantRecursively(entryKey)
            }
        }

        return secondInvariant && checkInvariantRecursively()
    }

    fun checkInvariant() {
        if (!checkInvariantRecursively(parentRegion = null)) {
            error("The invariant of region tree is violated!")
        }
    }

    private inner class RecursiveSplitResult(
        val completelyCoveredRegionTree: CompletelyCoveredRegionTree<Reg, Value>,
        val disjointRegionTree: DisjointRegionTree<Reg, Value>,
    ) {
        operator fun component1(): RegionTree<Reg, Value> = completelyCoveredRegionTree
        operator fun component2(): RegionTree<Reg, Value> = disjointRegionTree
    }

    //                                    Ordered map logic


    fun put(key: Reg, value: Pair<Value, RegionTree<Reg, Value>>) {
        if (isEmpty) {
            entries = entries.put(key, LinkedValue(value), owner = null)
            firstKey = key
            lastKey = key
            return
        }

        val links = entries[key]
        if (links != null) {
            if (links.value === value) {
                return
            }
            entries = entries.put(key, links.withValue(value), owner = null)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val lastKey = lastKey as Reg
        val lastLinks = entries[lastKey]!!
//        assert(!lastLink.hasNext)
        entries = entries
            .put(lastKey, lastLinks.withNext(key), owner = null)
            .put(key, LinkedValue(value, previous = lastKey), owner = null)
        this.lastKey = key
    }

    fun putAll(other: RegionTree<Reg, Value>) =
        other.rootEntriesIterator().forEach { (key, value) -> this.put(key, value) }

    private fun remove(key: Reg, ownership: MutabilityOwnership?) {
        val links = entries[key] ?: return

        entries = entries.remove(key, ownership)
        if (links.hasPrevious) {
            @Suppress("UNCHECKED_CAST")
            val previousLinks = entries[links.previous as Reg]!!
//            assert(previousLinks.next == key)
            entries = entries.put(links.previous, previousLinks.withNext(links.next), ownership)
        }
        if (links.hasNext) {
            @Suppress("UNCHECKED_CAST")
            val nextLinks = entries[links.next as Reg]!!
//            assert(nextLinks.previous == key)
            entries = entries.put(links.next, nextLinks.withPrevious(links.previous), ownership)
        }

        firstKey = if (!links.hasPrevious) links.next else firstKey
        lastKey = if (!links.hasNext) links.previous else lastKey
    }



    /**
     * [entriesIterators] should be considered as a recursion stack where
     * the last element is the deepest one in the branch we explore.
     */
    private inner class TheLeftestTopSortIterator private constructor(
        private val entriesIterators: MutableList<RegionTreeEntryIterator<Reg, Value>>,
    ) : Iterator<Pair<Value, Reg>> {
        // A stack of elements we should emit after we process all their children.
        // We cannot use for it corresponding iterators since every value from an
        // iterator can be retrieved only once, but we already got it when we discovered the previous layer.
        private val nodes: MutableList<Pair<Reg, Value>> = mutableListOf()

        constructor(iterator: RegionTreeEntryIterator<Reg, Value>) : this(mutableListOf(iterator))

        override fun hasNext(): Boolean {
            while (entriesIterators.isNotEmpty()) {
                val currentIterator = entriesIterators.last()

                if (!currentIterator.hasNext()) {
                    // We have nodes in the processing stack that we didn't emit yet
                    return nodes.isNotEmpty()
                }

                // We have elements to process inside the currentIterator
                return true
            }

            // Both iterators and nodes stacks are empty.
            return false
        }

        override fun next(): Pair<Value, Reg> {
            while (entriesIterators.isNotEmpty()) {
                val currentIterator = entriesIterators.last()

                // We reached an end of the current layer in the tree, go back to the previous one
                if (!currentIterator.hasNext()) {
                    entriesIterators.removeLast()
                    // We processed all the children, now we can emit their parent
                    return nodes.removeLast().let { it.second to it.first }
                }

                // Take the next element on the current layer
                val (key, valueWithRegionTree) = currentIterator.next()
                val value = valueWithRegionTree.first
                val regionTree = valueWithRegionTree.second

                // If it is a leaf, it is the answer, return it
                if (regionTree.isEmpty) {
                    return value to key
                }

                // Otherwise, add it in nodes list and an iterator for its children in the stack
                nodes += key to value
                entriesIterators += regionTree.rootEntriesIterator()
            }

            // That means that there are no unprocessed nodes in the tree
            throw NoSuchElementException()
        }
    }


    private inner class OrderedHashMapIterator : Iterator<Pair<Reg, Pair<Value, RegionTree<Reg, Value>>>> {
        private var current = firstKey
        override fun hasNext(): Boolean = current !== EndOfChain

        override fun next(): Pair<Reg, Pair<Value, RegionTree<Reg, Value>>> {
            @Suppress("UNCHECKED_CAST")
            val key = current as Reg
            val entry = entries[key]!!
            current = entry.next
            return key to entry.value
        }
    }

    fun rootEntriesIterator(): RegionTreeEntryIterator<Reg, Value> = OrderedHashMapIterator()

    /**
     * Returns an iterator that returns topologically sorted elements.
     * Note that elements from the same level will be processed in order from the
     * oldest entry to the most recently updated one.
     */
    override fun iterator(): Iterator<Pair<Value, Reg>> =
        TheLeftestTopSortIterator(this.rootEntriesIterator())

    override fun toString(): String = if (isEmpty) "emptyTree" else toString(balance = 0)

    private fun toString(balance: Int): String =
        rootEntriesIterator().asSequence().joinToString(separator = System.lineSeparator()) {
            val subtree = it.second.second
            val region = it.first
            val value = it.second.first
            val indent = "\t".repeat(balance)

            val subtreeString = if (subtree.isEmpty) {
                "\t" + indent + "emptyTree"
            } else {
                subtree.toString(balance + 1)
            }

            indent + "$region -> $value:${System.lineSeparator()}$subtreeString"
        }

}

private typealias DisjointRegionTree<Reg, Value> = RegionTree<Reg, Value>
private typealias CompletelyCoveredRegionTree<Reg, Value> = RegionTree<Reg, Value>
private typealias RegionTreeEntryIterator<Reg, Value> = Iterator<Pair<Reg, Pair<Value, RegionTree<Reg, Value>>>>

@Suppress("UNCHECKED_CAST")
fun <Reg : Region<Reg>, Value> emptyRegionTree(): RegionTree<Reg, Value> = RegionTree()
