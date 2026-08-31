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

    suspend fun getAll(userId: String): List<Transaction>
    suspend fun add(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)

    /** Pushes dirty local rows and tombstoned deletes to the remote table. */
    suspend fun syncPendingToRemote(userId: String)

    /** Pulls remote creates/updates/deletes into the local database (merge policy: SyncMerger). */
    suspend fun pullRemote(userId: String)
}
