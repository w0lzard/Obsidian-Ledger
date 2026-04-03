package com.ryuken.obsidianledger.features.analytics

import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class GetMonthlyTotalsUseCase(
    private val transactionRepo: TransactionRepository
) {
    /**
     * Returns the last [months] months of summary data for sparkline charts.
     * Each entry is (year, month, MonthlySummary).
     */
    operator fun invoke(
        userId: String,
        currentYear: Int,
        currentMonth: Int,
        months: Int = 6
    ): Flow<List<Triple<Int, Int, MonthlySummary>>> {
        val ranges = (0 until months).map { offset ->
            var y = currentYear
            var m = currentMonth - offset
            while (m <= 0) { m += 12; y -= 1 }
            y to m
        }.reversed()

        val flows = ranges.map { (y, m) ->
            transactionRepo.observeMonthlySummary(userId, y, m)
        }

        return if (flows.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(flows) { summaries ->
                summaries.mapIndexed { idx, summary ->
                    val (y, m) = ranges[idx]
                    Triple(y, m, summary)
                }
            }
        }
    }
}
