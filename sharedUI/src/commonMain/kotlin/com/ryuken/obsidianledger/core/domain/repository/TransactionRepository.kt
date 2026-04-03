package com.ryuken.obsidianledger.core.domain.repository

import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeByMonth(
        userId : String,
        year   : Int,
        month  : Int
    ): Flow<List<Transaction>>

    fun observeMonthlySummary(
        userId : String,
        year   : Int,
        month  : Int
    ): Flow<MonthlySummary>

    suspend fun add(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)
    suspend fun syncPendingToRemote(userId: String)
}
