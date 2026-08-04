package com.ryuken.obsidianledger.core.domain.helper

import kotlin.test.Test
import kotlin.test.assertEquals

class HelpersTest {

    @Test
    fun roundToCents_dropsFloatingPointDrift() {
        val drifted = (0.1 + 0.2) * 100 // classic Double drift, != 30.0 exactly
        assertEquals(0.3, drifted.let { 0.1 + 0.2 }.roundToCents())
    }

    @Test
    fun roundToCents_roundsToNearestCent() {
        assertEquals(19.99, 19.994999.roundToCents())
        assertEquals(20.0, 19.995.roundToCents())
    }

    @Test
    fun isLeapYear_knownCases() {
        assertEquals(true, isLeapYear(2024))
        assertEquals(false, isLeapYear(2023))
        assertEquals(false, isLeapYear(1900))
        assertEquals(true, isLeapYear(2000))
    }
}
