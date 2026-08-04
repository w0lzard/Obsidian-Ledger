package com.ryuken.obsidianledger.core.domain.helper

import kotlinx.datetime.Month
import kotlin.math.round

// ponytail: Double drifts past 2dp after repeated sums; round at every aggregation boundary
// instead of migrating amount storage to Long/BigDecimal.
fun Double.roundToCents(): Double = round(this * 100) / 100.0

fun Double.toCents(): Long = round(this * 100).toLong()

// Largest-remainder method: guarantees sum(result) == totalCents exactly.
fun distributeCentsEvenly(totalCents: Long, count: Int): List<Long> {
    if (count <= 0) return emptyList()
    val base = totalCents / count
    val remainder = (totalCents % count).toInt()
    return List(count) { index -> base + if (index < remainder) 1 else 0 }
}

fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

fun Month.length(isLeapYear: Boolean): Int {
    return when (this) {
        Month.FEBRUARY -> if (isLeapYear) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }
}
