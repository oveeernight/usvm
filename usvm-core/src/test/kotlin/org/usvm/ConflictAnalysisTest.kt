package org.usvm

import io.ksmt.expr.KExpr
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.BeforeEach
import org.usvm.api.readArrayIndex
import org.usvm.api.readArrayLength
import org.usvm.api.writeArrayIndex
import org.usvm.collections.immutable.internal.MutabilityOwnership
import org.usvm.constraints.UEqualityConstraints
import org.usvm.constraints.UTypeConstraints
import org.usvm.constraints.UTypeEvaluator
import org.usvm.memory.UMemory
import org.usvm.memory.UReadOnlyMemory
import org.usvm.memory.UReadOnlyRegistersStack
import org.usvm.memory.URegistersStack
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CompositionConflictsTest {
    private lateinit var stackEvaluator: UReadOnlyRegistersStack
    private lateinit var typeEvaluator: UTypeEvaluator<Type>
    private lateinit var mockEvaluator: UMockEvaluator
    private lateinit var memory: UReadOnlyMemory<Type>

    private lateinit var ctx: UContext<USizeSort>
    private lateinit var ownership: MutabilityOwnership
    private lateinit var composer: ConflictsComposer<Type, USizeSort>

    @BeforeEach
    fun initializeContext() {
        val components: UComponents<*, USizeSort> = mockk()
        every { components.mkTypeSystem(any()) } returns mockk()

        ctx = UContext(components)
        ownership = MutabilityOwnership()
        every { components.mkSizeExprProvider(any()) } answers { UBv32SizeExprProvider(ctx) }
        every { components.mkComposer(ctx) } answers {
            { memory: UReadOnlyMemory<Type>, ownership: MutabilityOwnership -> UComposer(ctx, memory, ownership) }
        }

        stackEvaluator = mockk()
        typeEvaluator = mockk()
        mockEvaluator = mockk()

        memory = mockk()
        every { memory.types } returns typeEvaluator
        every { memory.stack } returns stackEvaluator
        every { memory.mocker } returns mockEvaluator

        composer = ConflictsComposer(ctx, memory, ownership)
    }

    @Test
    fun singleConflictExprTest() = with(ctx) {
        val idx = 0
        val bv32sort = mkBv32Sort()
        val i = mkRegisterReading(0, bv32sort)
        val value = mkBv(10)
        every { stackEvaluator.readRegister(idx, bv32sort) } returns value

        val expr = i eq mkBv(11)
        var conflicts = composer.conflictingAtoms(expr)
        assertSame(expr, conflicts[0])

        val negExpr = i neq value
        conflicts = composer.conflictingAtoms(negExpr)
        assertSame(negExpr, conflicts[0])

        val trueExpr = i eq value
        conflicts = composer.conflictingAtoms(trueExpr)
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

        val conflicts = composer.conflictingAtoms(isExpression)
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
        val conflicts = composer.conflictingAtoms(trueExpr)
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
        var conflicts = composer.conflictingAtoms(orExpr)
        assertEquals(2, conflicts.size)
        assertContains(conflicts, a)
        assertContains(conflicts, b)

        val andExpr = a and b
        conflicts = composer.conflictingAtoms(andExpr)
        assertEquals(2, conflicts.size)
        assertContains(conflicts, a)
        assertContains(conflicts, b)
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
        var conflicts = composer.conflictingAtoms(negAndExpr1)
        assertEquals(2, conflicts.size)
        assertContains(conflicts, a)
        assertContains(conflicts, b)

        val c = i neq iValue
        val d = i eq jValue
        val negAndExpr2 = mkNot(c and d)
        conflicts = composer.conflictingAtoms(negAndExpr2)
        assertEquals(0, conflicts.size)

        val e = i eq iValue
        val f = j neq jValue
        val negOrExpr = mkNot(e or f)
        conflicts = composer.conflictingAtoms(negOrExpr)
        assertEquals(1, conflicts.size)
        assertContains(conflicts, e)
    }

    @Test
    fun nestedBinaryOperationsConflictsTest() = with(ctx) {
        // a[i] == 42 and (a[j] == 0 || a[j] == 1)
        val arrayType: KClass<Array<*>> = Array::class
        val bv32sort = mkBv32Sort()
        val address = spyk(mkRegisterReading(0, addressSort))
        val fstIndex = spyk(mkRegisterReading(1, sizeSort))
        val sndIndex = spyk(mkRegisterReading(2, sizeSort))
        val thdIndex = spyk(mkRegisterReading(3, sizeSort))

        val memory = UMemory<KClass<*>, Any>(ctx, mockk(), mockk())
        memory.writeArrayIndex(address, fstIndex, arrayType, bv32sort, 42.toBv(), guard = trueExpr)
        memory.writeArrayIndex(address, sndIndex, arrayType, bv32sort, 43.toBv(), guard = trueExpr)

        // TODO replace with jacoDB type
        val fstArrayIndexReading = memory.readArrayIndex(address, fstIndex, arrayType, bv32sort)
        val thdArrayIndexReading = memory.readArrayIndex(address, thdIndex, arrayType, bv32sort)

        val composer = ConflictsComposer(ctx, memory, ownership) // TODO replace with jacoDB type

        every { address.accept(composer) } returns address
        every { fstIndex.accept(composer) } returns fstIndex
        every { sndIndex.accept(composer) } returns sndIndex
        every { thdIndex.accept(composer) } returns sndIndex

        val a = fstArrayIndexReading eq 42.toBv()
        val b = thdArrayIndexReading eq 0.toBv()
        val c = thdArrayIndexReading eq 1.toBv()
        val expr = a and (b or c)
        val conflicts = composer.conflictingAtoms(expr)

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
        val g = mockk<UBoolExpr>()
        val h = mockk<UBoolExpr>()

        every { a.ctx } returns ctx
        every { b.ctx } returns ctx
        every { c.ctx } returns ctx
        every { d.ctx } returns ctx
        every { e.ctx } returns ctx
        every { f.ctx } returns ctx
        every { g.ctx } returns ctx
        every { h.ctx } returns ctx

        every { a.accept(any()) } returns trueExpr
        every { b.accept(any()) } returns trueExpr
        every { c.accept(any()) } returns falseExpr
        every { d.accept(any()) } returns trueExpr
        every { e.accept(any()) } returns falseExpr
        every { f.accept(any()) } returns falseExpr
        every { g.accept(any()) } returns trueExpr
        every { h.accept(any()) } returns falseExpr

        // !((a and b) and (c or d) and !(e or f )
        val expr = !((a and b) and (c or d) and !(e or f) and !(g and h))
        val conflicts = composer.conflictingAtoms(expr)
        assertEquals(6, conflicts.size)
        val expectedConflicts = listOf(a, b, d, e, f, h)
        expectedConflicts.forEach { assertContains(conflicts, it) }
    }
}

class ConflictsLocalizationTest {
    private lateinit var memory: UMemory<Type, Any>

    private lateinit var ctx: UContext<USizeSort>
    private lateinit var initOwnership: MutabilityOwnership
    private lateinit var arrayDescr: Pair<Type, UAddressSort>

    @BeforeEach
    fun initializeContext() {
        val components: UComponents<Type, USizeSort> = mockk()
        every { components.mkTypeSystem(any()) } returns mockk()

        ctx = UContext(components)
        initOwnership = MutabilityOwnership()
        every { components.mkSizeExprProvider(any()) } answers { UBv32SizeExprProvider(ctx) }
        every { components.mkComposer(ctx) } answers {
            { memory: UReadOnlyMemory<Type>, ownership: MutabilityOwnership -> UComposer(ctx, memory, ownership) }
        }

        val eqConstraints = UEqualityConstraints(ctx, initOwnership)
        val typeConstraints = UTypeConstraints(initOwnership, components.mkTypeSystem(ctx), eqConstraints)

        memory = UMemory(ctx, initOwnership, typeConstraints)
        arrayDescr = mockk<Type>() to ctx.addressSort
    }

    @Test
    fun singleConflict() = with(ctx) {
        val ref = mkRegisterReading(0, addressSort)
        val idx = mkRegisterReading(1, sizeSort)
        val bvSort = mkBv32Sort()

        memory.writeArrayIndex(ref, idx, arrayDescr,  bvSort, 1.toBv(), trueExpr)

        val composer = spyk(ConflictsComposer(ctx, memory, initOwnership))

        val otherIdx = mkRegisterReading(2, sizeSort)
        val reading = memory.readArrayIndex(ref, otherIdx, arrayDescr, bvSort)

        every { composer.transform(otherIdx) } returns idx
        every { composer.transform(idx) } returns idx
        every { composer.transform(ref) } returns ref

        val expr = reading eq 0.toBv()
        val conflictingExprs = composer.conflictingAtoms(expr)
        assertEquals(1, conflictingExprs.size)
        assertContains(conflictingExprs, expr)
        val ownerships = composer.getConflictingOwnerships(expr)
        assertEquals(1, ownerships.size)
        assertContains(ownerships, initOwnership)
    }

    @Test
    fun testStackConflict() = with(ctx) {
        val i = mkRegisterReading(0, sizeSort)
        val j = mkRegisterReading(1, sizeSort)

        val stack = URegistersStack()
        // TODO ownership
        stack.writeRegister(0, mkBv(0))
        stack.writeRegister(1, mkBv(1))

        val memory = UMemory<KClass<*>, Any>(ctx, mockk(), mockk(), stack)
        val composer = spyk(ConflictsComposer(ctx, memory, initOwnership))
        val expr = (i eq mkBv(0)) or


        every { composer.compose(i) } returns mkBv(0)
        val expr
    }

    @Test
    fun severalOwnershipsConflict() = with(ctx) {
        val ref = mkRegisterReading(0, addressSort)
        val idx1 = mkRegisterReading(1, sizeSort)
        val idx2 = mkRegisterReading(2, sizeSort)
        val bvSort = mkBv32Sort()

        memory.writeArrayIndex(ref, idx1, arrayDescr,  bvSort, 1.toBv(), trueExpr)
        val (thisOwnership, cloneOwnership) = MutabilityOwnership() to MutabilityOwnership()
        val memoryClone = memory.clone(memory.types, thisOwnership, cloneOwnership)

        memory.writeArrayIndex(ref, idx2, arrayDescr,  bvSort, 2.toBv(), trueExpr)
        val composer = spyk(ConflictsComposer(ctx, memory, initOwnership))

        val otherIdx = mkRegisterReading(3, sizeSort)
        val reading = memory.readArrayIndex(ref, otherIdx, arrayDescr, bvSort)

        every { composer.transform(otherIdx) } returns idx2
        every { composer.transform(idx1) } returns idx1
        every { composer.transform(idx2) } returns idx2
        every { composer.transform(ref) } returns ref

        val expr = reading eq 0.toBv()
        val conflictingExprs = composer.conflictingAtoms(expr)
        assertEquals(1, conflictingExprs.size)
        assertContains(conflictingExprs, expr)
        val ownerships = composer.getConflictingOwnerships(expr)
        assertEquals(1, ownerships.size)
        assertContains(ownerships, thisOwnership)


        memoryClone.writeArrayIndex(ref, idx2, arrayDescr,  bvSort, 2.toBv(), trueExpr)
        val cloneComposer = spyk(ConflictsComposer(ctx, memoryClone, initOwnership))
        val cloneReading = memoryClone.readArrayIndex(ref, idx1, arrayDescr, bvSort)


        every { cloneComposer.apply(idx2) } returns 2.toBv(bv32Sort)
        every { cloneComposer.apply(idx1) } returns 1.toBv(bv32Sort)
        every { cloneComposer.apply(ref) } returns ref

        val cloneExpr = cloneReading eq 1.toBv()
        val cloneConflictingExprs = cloneComposer.conflictingAtoms(expr)
        assertEquals(1, cloneConflictingExprs.size)
        assertContains(cloneConflictingExprs, cloneExpr)
        val cloneOwnerships = composer.getConflictingOwnerships(cloneExpr)
        assertEquals(1, cloneOwnerships.size)
        assertContains(cloneOwnerships, initOwnership)
    }
}
