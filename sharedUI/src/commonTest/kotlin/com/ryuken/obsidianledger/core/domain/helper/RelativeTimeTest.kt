package com.ryuken.obsidianledger.core.domain.helper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class RelativeTimeTest {

    private val now = Instant.parse("2026-09-01T12:00:00Z")

    @Test
    fun nullAndBlank_renderNever() {
        assertEquals("Never", relativeTime(null, now))
        assertEquals("Never", relativeTime("", now))
    }

    @Test
    fun legacyLiteral_renderNever() {
        assertEquals("Never", relativeTime("Just now", now))
    }

    @Test
    fun underOneMinute_justNow() {
        assertEquals("Just now", relativeTime((now - 30.seconds).toString(), now))
    }

    @Test
    fun minutesAgo() {
        assertEquals("5 min ago", relativeTime((now - 5.minutes).toString(), now))
    }

    @Test
    fun hoursAgo() {
        assertEquals("3 hr ago", relativeTime((now - 3.hours).toString(), now))
    }

    @Test
    fun daysAgo_singularAndPlural() {
        assertEquals("1 day ago", relativeTime((now - 1.days).toString(), now))
        assertEquals("3 days ago", relativeTime((now - 3.days).toString(), now))
    }

    @Test
    fun weeksAgo() {
        assertEquals("2 weeks ago", relativeTime((now - 14.days).toString(), now))
    }

    @Test
    fun futureTimestamp_clockSkewGuard_justNow() {
        assertEquals("Just now", relativeTime((now + 10.minutes).toString(), now))
    }
}
