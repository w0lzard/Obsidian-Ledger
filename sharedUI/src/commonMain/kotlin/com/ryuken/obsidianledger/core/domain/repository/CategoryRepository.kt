// CategoryRepository.kt
package com.ryuken.obsidianledger.core.domain.repository

import com.ryuken.obsidianledger.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(userId: String): Flow<List<Category>>
    suspend fun insertCustom(category: Category, userId: String)
    suspend fun delete(id: String)
    suspend fun getDefaultCategory(id: String): Category
}