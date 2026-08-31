// CategoryRepository.kt
package com.ryuken.obsidianledger.core.domain.repository

import com.ryuken.obsidianledger.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(userId: String): Flow<List<Category>>
    suspend fun insertCustom(category: Category, userId: String)

    /** Soft-deletes a custom category; throws if still referenced by transactions or budgets. */
    suspend fun delete(id: String)
    suspend fun getDefaultCategory(id: String): Category

    /** Pushes dirty custom categories and tombstoned deletes to the remote table. */
    suspend fun syncPendingToRemote(userId: String)

    /** Pulls remote custom categories into the local database (merge policy: SyncMerger). */
    suspend fun pullRemote(userId: String)
}