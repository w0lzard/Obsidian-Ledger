package com.ryuken.obsidianledger.core.domain.helper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneyTest {

    @Test
    fun roundToCents_boundaries() {
        assertEquals(10.01, 10.005.roundToCents())
        assertEquals(10.0, 10.0.roundToCents())
        assertEquals(10.12, 10.123456.roundToCents())
        assertEquals(-3.14, (-3.135).roundToCents())
    }

    @Test
    fun roundToCents_isStableUnderRerounding() {
        // The property the whole Double+boundary scheme relies on.
        var v = (0.1 + 0.2).roundToCents()          // 0.3, drift removed
        repeat(10) { v = v.roundToCents() }
        assertEquals(0.3, v)
    }

    @Test
    fun roundToCents_dropsFloatingPointDrift_inSums() {
        val sum = (1..10).sumOf { 0.1 }
        assertTrue(0.9999999999999999 == sum)   // raw drift is real
        assertEquals(1.0, sum.roundToCents())    // boundary fixes it
    }

    @Test
    fun largeMagnitude_stillExact() {
        assertEquals(90_000_000_000.12, 90_000_000_000.123.roundToCents())
    }

    @Test
    fun distributeCentsEvenly_sumsExactly() {
        val totalCents = 10_001L
        val parts = distributeCentsEvenly(totalCents, 3)
        assertEquals(totalCents, parts.sum())
        assertEquals(3_334L, parts.max())
        assertEquals(3_333L, parts.min())
    }

    @Test
    fun distributeCentsEvenly_manyMembers() {
        val totalCents = 99L
        val parts = distributeCentsEvenly(totalCents, 7)
        assertEquals(totalCents, parts.sum())
        assertTrue(parts.all { it in (totalCents / 7)..(totalCents / 7 + 1) })
    }

    @Test
    fun centsRoundTripThroughDouble() {
        // toCents -> /100 -> toCents is the push/pull path through Postgres.
        val amounts = listOf(0.01, 0.1, 10.01, 123.45, 9999.99)
        amounts.forEach { a ->
            assertEquals(a, (a.toCents() / 100.0).roundToCents(), "round trip failed for $a")
        }
    }
}
