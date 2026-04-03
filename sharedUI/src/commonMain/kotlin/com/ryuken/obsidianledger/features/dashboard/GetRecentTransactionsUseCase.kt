package com.ryuken.obsidianledger.features.dashboard

import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetRecentTransactionsUseCase(
    private val transactionRepo: TransactionRepository
) {
    operator fun invoke(
        userId: String,
        year: Int,
        month: Int,
        limit: Int = 10
    ): Flow<List<Transaction>> {
        return transactionRepo.observeByMonth(userId, year, month)
            .map { it.take(limit) }
    }
}
