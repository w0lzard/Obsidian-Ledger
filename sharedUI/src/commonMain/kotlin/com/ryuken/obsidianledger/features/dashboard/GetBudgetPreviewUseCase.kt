package com.ryuken.obsidianledger.features.dashboard

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetBudgetPreviewUseCase(
    private val budgetRepo: BudgetRepository
) {
    operator fun invoke(
        userId: String,
        year: Int,
        month: Int,
        maxBudgets: Int = 3
    ): Flow<List<Budget>> {
        return budgetRepo.observeBudgetsWithSpending(userId, year, month)
            .map { it.take(maxBudgets) }
    }
}
