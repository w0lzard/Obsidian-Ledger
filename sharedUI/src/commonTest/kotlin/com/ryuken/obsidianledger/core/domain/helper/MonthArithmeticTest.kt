package com.ryuken.obsidianledger.core.domain.helper

import kotlin.test.Test
import kotlin.test.assertEquals

class MonthArithmeticTest {

    @Test
    fun nextMonth_midYear() {
        assertEquals(2026 to 7, nextMonth(2026, 6))
    }

    @Test
    fun nextMonth_decemberRollsYear() {
        assertEquals(2027 to 1, nextMonth(2026, 12))
    }

    @Test
    fun previousMonth_midYear() {
        assertEquals(2026 to 5, previousMonth(2026, 6))
    }

    @Test
    fun previousMonth_januaryRollsYearBack() {
        assertEquals(2025 to 12, previousMonth(2026, 1))
    }

    @Test
    fun rapidNavigationRoundTrips() {
        var cursor = 2026 to 6
        repeat(50) { cursor = nextMonth(cursor.first, cursor.second) }
        repeat(50) { cursor = previousMonth(cursor.first, cursor.second) }
        assertEquals(2026 to 6, cursor)
    }
}
