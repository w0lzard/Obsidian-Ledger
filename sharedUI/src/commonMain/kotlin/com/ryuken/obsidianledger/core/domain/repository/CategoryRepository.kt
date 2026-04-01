package com.ryuken.obsidianledger.core.domain.repository

import com.ryuken.obsidianledger.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(userId: String): Flow<List<Category>>
    suspend fun insertCategory(category: Category)
    suspend fun deleteCategory(id: String)
}
