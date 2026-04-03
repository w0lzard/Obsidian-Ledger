package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository

class SyncUseCase(
    private val transactionRepo: TransactionRepository,
    private val budgetRepo: BudgetRepository
) {
    suspend operator fun invoke(userId: String) {
        transactionRepo.syncPendingToRemote(userId)
        budgetRepo.syncPendingToRemote(userId)
    }
}
