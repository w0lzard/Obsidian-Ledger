package com.ryuken.obsidianledger.features.analytics

import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.fake.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class GetMonthlyTotalsUseCaseTest {

    @Test
    fun sixMonths_rollsBackAcrossJanuary() = runBlocking {
        val repo = FakeTransactionRepository()
        repo.monthlySummaries.value = mapOf(
            (2026 to 2) to MonthlySummary(2.0, 0.0, emptyMap(), transactionCount = 1),
            (2025 to 9) to MonthlySummary(9.0, 0.0, emptyMap(), transactionCount = 1)
        )
        val totals = GetMonthlyTotalsUseCase(repo)(userId = "u", currentYear = 2026, currentMonth = 2, months = 6).first()

        assertEquals(6, totals.size)
        // Oldest first, ending on the requested month.
        assertEquals(
            listOf(2025 to 9, 2025 to 10, 2025 to 11, 2025 to 12, 2026 to 1, 2026 to 2),
            totals.map { it.first to it.second }
        )
        assertEquals(9.0, totals.first().third.totalExpense)
        assertEquals(2.0, totals.last().third.totalExpense)
    }

    @Test
    fun singleMonth_noRollover() = runBlocking {
        val totals = GetMonthlyTotalsUseCase(FakeTransactionRepository())("u", 2026, 7, months = 1).first()
        assertEquals(listOf(2026 to 7), totals.map { it.first to it.second })
    }
}
