package org.usvm

import io.ksmt.expr.KExpr
import io.ksmt.utils.getValue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.usvm.api.allocateConcreteRef
import org.usvm.api.readArrayIndex
import org.usvm.api.writeArrayIndex
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
        val arrayType: KClass<Array<*>> = Array::class
        val bv32sort = mkBv32Sort()
        val address = mkRegisterReading(0, addressSort)
        val fstIndex = mkRegisterReading(1, sizeSort)
        val sndIndex = mkRegisterReading(2, sizeSort)
        val thdIndex = mkRegisterReading(3, sizeSort)

        val memory = UMemory<KClass<*>, Any>(ctx, mockk(), mockk())
        memory.writeArrayIndex(address, fstIndex, arrayType, bv32sort, 42.toBv(), guard = trueExpr)
        memory.writeArrayIndex(address, sndIndex, arrayType, bv32sort, 43.toBv(), guard = trueExpr)

        // TODO replace with jacoDB type
        val fstArrayIndexReading = memory.readArrayIndex(address, fstIndex, arrayType, bv32sort)
        val thdArrayIndexReading = memory.readArrayIndex(address, thdIndex, arrayType, bv32sort)

        val composer = UComposer(ctx, memory) // TODO replace with jacoDB type

        every { address.accept(composer) } returns address
        every { fstIndex.accept(composer) } returns fstIndex
        every { sndIndex.accept(composer) } returns sndIndex
        every { thdIndex.accept(composer) } returns sndIndex

        val a = fstArrayIndexReading eq 42.toBv()
        val b = thdArrayIndexReading eq 0.toBv()
        val c = thdArrayIndexReading eq 1.toBv()
        val expr = a and (b or c)
        val conflicts = composer.collectConflicts(expr)

        assertEquals(2, conflicts.size)
        assertContains(conflicts, b)
        assertContains(conflicts, c)
    }

    @Test
    fun invertingConflictingValuesTest() = with(ctx) {
        val a = mockk<UBoolExpr>()
        val b = mockk<UBoolExpr>()
        val c = mockk<UBoolExpr>()
        val d = mockk<UBoolExpr>()
        val e = mockk<UBoolExpr>()
        val f = mockk<UBoolExpr>()

        every { a.ctx } returns ctx
        every { b.ctx } returns ctx
        every { c.ctx } returns ctx
        every { d.ctx } returns ctx
        every { e.ctx } returns ctx
        every { f.ctx } returns ctx

        every { a.accept(any()) } returns trueExpr
        every { b.accept(any()) } returns trueExpr
        every { c.accept(any()) } returns falseExpr
        every { d.accept(any()) } returns trueExpr
        every { e.accept(any()) } returns falseExpr
        every { f.accept(any()) } returns falseExpr

        // (! (a and b) and (c or d) and !(e or f )
        val expr = !((a and b) and (c or d) and !(e or f))
        val conflicts = composer.collectConflicts(expr)
        assertEquals(5, conflicts.size)
        val expectedConflicts = listOf(a, b, d, e, f)
        expectedConflicts.forEach { assertContains(conflicts, it) }
    }
}
