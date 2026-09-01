package com.ryuken.obsidianledger.core.domain.helper

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Human-facing "how long ago" for the stored last-sync instant. Pure — `now` is
 * injected so boundary behaviour is testable. Unparseable input (legacy values,
 * empty) renders as "Never".
 */
fun relativeTime(lastSync: String?, now: Instant = kotlin.time.Clock.System.now()): String {
    if (lastSync.isNullOrBlank()) return "Never"
    val instant = runCatching { Instant.parse(lastSync) }.getOrNull() ?: return "Never"
    val delta: Duration = now - instant
    return when {
        delta < Duration.ZERO        -> "Just now"          // clock skew guard
        delta.inWholeMinutes < 1     -> "Just now"
        delta.inWholeMinutes < 60    -> "${delta.inWholeMinutes} min ago"
        delta.inWholeHours < 24      -> "${delta.inWholeHours} hr ago"
        delta.inWholeDays < 7        -> "${delta.inWholeDays} day${if (delta.inWholeDays == 1L) "" else "s"} ago"
        else                         -> "${delta.inWholeDays / 7} week${if (delta.inWholeDays / 7 == 1L) "" else "s"} ago"
    }
}
