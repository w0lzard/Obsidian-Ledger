package com.ryuken.obsidianledger.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import com.ryuken.obsidianledger.core.domain.helper.isLeapYear
import com.ryuken.obsidianledger.core.domain.helper.length

data class Budget(
    val id: String,
    val category: Category,
    val limitAmount: Double,
    val spent: Double,
    val period: BudgetPeriod,
    val userId: String,
    val isDirty: Boolean = true
) {
    val percentUsed: Double
        get() = if (limitAmount > 0) (spent / limitAmount) * 100.0 else 0.0

    val remaining: Double
        get() = (limitAmount - spent).coerceAtLeast(0.0)

    val isOverBudget: Boolean
        get() = spent > limitAmount

    val status: BudgetStatus
        get() = when {
            percentUsed >= 100 -> BudgetStatus.EXCEEDED
            percentUsed >= 95 -> BudgetStatus.CRITICAL
            percentUsed >= 80 -> BudgetStatus.HIGH_ALERT
            percentUsed >= 60 -> BudgetStatus.WARNING
            else -> BudgetStatus.HEALTHY
        }

    val daysRemainingInPeriod: Int
        get() {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val days = when (period) {
                BudgetPeriod.MONTHLY -> {
                    val lastDay = LocalDate(
                        today.year,
                        today.month,
                        today.month.length(isLeapYear(today.year))
                    )
                    (lastDay.toEpochDays() - today.toEpochDays()).toInt()
                }
                BudgetPeriod.WEEKLY -> {
                    val dayOfWeek = today.dayOfWeek.ordinal // 0 = Monday
                    6 - dayOfWeek
                }
            }
            return days.coerceAtLeast(0)
        }

    val dailyAllowance: Double
        get() {
            val days = daysRemainingInPeriod
            return if (days > 0) remaining / days else 0.0
        }
}
