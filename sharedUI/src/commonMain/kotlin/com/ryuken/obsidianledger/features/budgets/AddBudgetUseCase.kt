package com.ryuken.obsidianledger.features.budgets

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository

class AddBudgetUseCase(
    private val budgetRepo: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) {
        budgetRepo.add(budget)
    }
}
