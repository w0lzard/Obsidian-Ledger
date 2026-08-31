package com.ryuken.obsidianledger.features.dashboard

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

/**
 * Full budget list for the month — the ViewModel slices the preview strip AND sums
 * the monthly total from this one source, so the aggregate never depends on the
 * preview slice size (the old bug: total summed `take(3)`).
 */
class GetBudgetPreviewUseCase(
    private val budgetRepo: BudgetRepository
) {
    operator fun invoke(
        userId: String,
        year: Int,
        month: Int
    ): Flow<List<Budget>> = budgetRepo.observeBudgetsWithSpending(userId, year, month)
}
