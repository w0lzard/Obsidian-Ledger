package com.ryuken.obsidianledger.core.domain.helper

/**
 * Pure month navigation used by Dashboard/Analytics intent handling — kept out of the
 * ViewModels so rollover logic is deterministically testable.
 */
fun nextMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) (year + 1) to 1 else year to (month + 1)

fun previousMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) (year - 1) to 12 else year to (month - 1)
