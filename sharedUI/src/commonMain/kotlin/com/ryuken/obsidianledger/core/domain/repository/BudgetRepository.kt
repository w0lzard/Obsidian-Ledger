// BudgetRepository.kt
package com.ryuken.obsidianledger.core.domain.repository

import com.ryuken.obsidianledger.core.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeBudgetsWithSpending(
        userId : String,
        year   : Int,
        month  : Int
    ): Flow<List<Budget>>
    suspend fun add(budget: Budget)
    suspend fun delete(id: String)

    /** Pushes dirty local rows and tombstoned deletes to the remote table. */
    suspend fun syncPendingToRemote(userId: String)

    /** Pulls remote creates/updates/deletes into the local database (merge policy: SyncMerger). */
    suspend fun pullRemote(userId: String)
}