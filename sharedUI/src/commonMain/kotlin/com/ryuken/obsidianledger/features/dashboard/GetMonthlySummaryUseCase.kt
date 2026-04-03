package com.ryuken.obsidianledger.features.dashboard

import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetMonthlySummaryUseCase(
    private val transactionRepo: TransactionRepository
) {
    operator fun invoke(userId: String, year: Int, month: Int): Flow<MonthlySummary> {
        return transactionRepo.observeMonthlySummary(userId, year, month)
    }
}
