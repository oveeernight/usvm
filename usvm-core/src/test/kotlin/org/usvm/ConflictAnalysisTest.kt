package org.usvm

import io.ksmt.expr.KExpr
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.usvm.collection.array.UInputArrayId
import org.usvm.collection.array.USymbolicArrayIndex
import org.usvm.constraints.UTypeEvaluator
import org.usvm.memory.*
import org.usvm.regions.SetRegion
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ConflictAnalysisTest {
    private lateinit var stackEvaluator: UReadOnlyRegistersStack
    private lateinit var typeEvaluator: UTypeEvaluator<Type>
    private lateinit var mockEvaluator: UMockEvaluator
    private lateinit var memory: UReadOnlyMemory<Type>

    private lateinit var ctx: UContext<USizeSort>
    private lateinit var concreteNull: UConcreteHeapRef
    private lateinit var composer: UComposer<Type, USizeSort>

    @BeforeEach
    fun initializeContext() {
        val components: UComponents<*, USizeSort> = mockk()
        every { components.mkTypeSystem(any()) } returns mockk()

        ctx = UContext(components)
        every { components.mkSizeExprProvider(any()) } answers { UBv32SizeExprProvider(ctx) }
        every { components.mkComposer(ctx) } answers { { memory: UReadOnlyMemory<Type> -> UComposer(ctx, memory) } }

        concreteNull = ctx.mkConcreteHeapRef(NULL_ADDRESS)
        stackEvaluator = mockk()
        typeEvaluator = mockk()
        mockEvaluator = mockk()

        memory = mockk()
        every { memory.types } returns typeEvaluator
        every { memory.stack } returns stackEvaluator
        every { memory.mocker } returns mockEvaluator

        composer = UComposer(ctx, memory)
    }

    @Test
    fun singleConflictExprTest() = with(ctx) {
        val idx = 0
        val bv32sort = mkBv32Sort()
        val i = mkRegisterReading(0, bv32sort)
        val value = mkBv(10)
        every { stackEvaluator.readRegister(idx, bv32sort) } returns value

        val expr = i eq mkBv(11)
        var conflicts = composer.collectConflicts(expr)
        assertSame(expr, conflicts[0])

        val negExpr = i neq value
        conflicts = composer.collectConflicts(negExpr)
        assertSame(negExpr, conflicts[0])

        val trueExpr = i eq value
        conflicts = composer.collectConflicts(trueExpr)
        assertEquals(0, conflicts.size)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun typedSingleConflictExprTest() = with(ctx) {
        val typeResult = mkFalse()
        val address = mkBv(1)
        val heapRef = mockk<UHeapRef>(relaxed = true)
        val type = mockk<KClass<*>>(relaxed = true) // TODO replace with jacoDB type
        val isExpression = mkIsSubtypeExpr(heapRef, type)

        every { heapRef.accept(any()) } returns address as KExpr<UAddressSort>
        every { typeEvaluator.evalIsSubtype(address, type) } returns typeResult

        val conflicts = composer.collectConflicts(isExpression)
        assertEquals(1, conflicts.size)
        assertSame(isExpression, conflicts[0])
    }

    @Test
    fun nonConflictingExprTest() = with(ctx) {
        val idx = 0
        val bv32sort = mkBv32Sort()
        val i = mkRegisterReading(0, bv32sort)
        val value = mkBv(10)
        every { stackEvaluator.readRegister(idx, bv32sort) } returns value

        val trueExpr = i eq value
        val conflicts = composer.collectConflicts(trueExpr)
        assertEquals(0, conflicts.size)
    }

    @Test
    fun simpleBinaryOperationConflictsTest() = with(ctx) {
        val idx = 0
        val bv32sort = mkBv32Sort()
        val i = mkRegisterReading(0, bv32sort)
        val value = mkBv(10)
        every { stackEvaluator.readRegister(idx, bv32sort) } returns value
        val a = i eq mkBv(9)
        val b = i eq mkBv(11)
        val orExpr = a or b
        var conflicts = composer.collectConflicts(orExpr)
        assertSame(b, conflicts[0])
        assertSame(a, conflicts[1])

        val andExpr = a and b
        conflicts = composer.collectConflicts(andExpr)
        assertSame(b, conflicts[0])
        assertSame(a, conflicts[1])
    }

    @Test
    fun simpleNegatedBinaryOperationConflictsTest() = with(ctx) {
        val idx1 = 0
        val bv32sort = mkBv32Sort()
        val i = mkRegisterReading(0, bv32sort)
        val iValue = mkBv(0)
        every { stackEvaluator.readRegister(idx1, bv32sort) } returns iValue

        val idx2 = 1
        val j = mkRegisterReading(1, bv32sort)
        val jValue = mkBv(1)
        every { stackEvaluator.readRegister(idx2, bv32sort) } returns jValue

        val a = i eq iValue
        val b = j eq jValue
        val negAndExpr1= mkNot(a and b)
        var conflicts = composer.collectConflicts(negAndExpr1)
        assertEquals(2, conflicts.size)
        assertContains(conflicts, a)
        assertContains(conflicts, b)

        val c = i neq iValue
        val d = i eq jValue
        val negAndExpr2 = mkNot(c and d)
        conflicts = composer.collectConflicts(negAndExpr2)
        assertEquals(0, conflicts.size)

        val e = i eq iValue
        val f = j neq jValue
        val negOrExpr = mkNot(e or f)
        conflicts = composer.collectConflicts(negOrExpr)
        assertEquals(1, conflicts.size)
        assertContains(conflicts, e)
    }

    @Test
    fun nestedBinaryOperationsConflictsTest() = with(ctx) {
        // a[i] == 42 and (a[j] == 0 || a[j] == 1)

        val address = mockk<UHeapRef>()
        val fstIndex = mockk<UExpr<USizeSort>>()
        val sndIndex = mockk<UExpr<USizeSort>>()

        val keyEqualityComparer = { k1: USymbolicArrayIndex<USizeSort>, k2: USymbolicArrayIndex<USizeSort> ->
            mkAnd((k1.first == k2.first).expr, (k1.second == k2.second).expr)
        }

        val keyInfo = object : TestKeyInfo<USymbolicArrayIndex<USizeSort>, SetRegion<USymbolicArrayIndex<USizeSort>>> {
            override fun mapKey(key: USymbolicArrayIndex<USizeSort>, transformer: UTransformer<*, *>?): USymbolicArrayIndex<USizeSort> =
                transformer.apply(key.first) to transformer.apply(key.second)

            override fun cmpConcreteLe(key1: USymbolicArrayIndex<USizeSort>, key2: USymbolicArrayIndex<USizeSort>): Boolean = key1 == key2
            override fun eqSymbolic(ctx: UContext<*>, key1: USymbolicArrayIndex<USizeSort>, key2: USymbolicArrayIndex<USizeSort>): UBoolExpr =
                keyEqualityComparer(key1, key2)
        }

        val updates1 = UFlatUpdates<USymbolicArrayIndex<USizeSort>, UBv32Sort>(keyInfo)
            .write(address to fstIndex, 42.toBv(), guard = trueExpr)
        val updates2 = UFlatUpdates<USymbolicArrayIndex<USizeSort>, UBv32Sort>(keyInfo)
            .write(address to sndIndex, 43.toBv(), guard = trueExpr)

        val arrayType: KClass<Array<*>> = Array::class

        val region1 = USymbolicCollection(UInputArrayId(arrayType, bv32Sort), updates1)
        val region2 = USymbolicCollection(UInputArrayId(arrayType, bv32Sort), updates2)

        // TODO replace with jacoDB type
        val fstArrayIndexReading = mkInputArrayReading(region1, address, fstIndex)
        // TODO replace with jacoDB type
        val sndArrayIndexReading = mkInputArrayReading(region2, address, sndIndex)

        val composer = UComposer(ctx, UMemory<KClass<*>, Any>(ctx, mockk())) // TODO replace with jacoDB type

        every { address.accept(composer) } returns address
        every { fstIndex.accept(composer) } returns fstIndex
        every { sndIndex.accept(composer) } returns sndIndex

        val a = fstArrayIndexReading eq 42.toBv()
        val b = sndArrayIndexReading eq 0.toBv()
        val c = sndArrayIndexReading eq 1.toBv()
        val expr = a and (b or c)
        val conflicts = composer.collectConflicts(expr)

        assertEquals(2, conflicts.size)
        assertContains(conflicts, b)
        assertContains(conflicts, c)
    }
}
