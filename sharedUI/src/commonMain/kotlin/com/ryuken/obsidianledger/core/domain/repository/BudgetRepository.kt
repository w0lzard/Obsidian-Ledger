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
    suspend fun syncPendingToRemote(userId: String)
}