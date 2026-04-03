package com.ryuken.obsidianledger.features.budgets

import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository

class DeleteBudgetUseCase(
    private val budgetRepo: BudgetRepository
) {
    suspend operator fun invoke(id: String) {
        budgetRepo.delete(id)
    }
}
