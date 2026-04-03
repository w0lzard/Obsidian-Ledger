package com.ryuken.obsidianledger.features.budgets

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgetsWithSpendingUseCase(
    private val budgetRepo: BudgetRepository
) {
    operator fun invoke(userId: String, year: Int, month: Int): Flow<List<Budget>> {
        return budgetRepo.observeBudgetsWithSpending(userId, year, month)
    }
}
