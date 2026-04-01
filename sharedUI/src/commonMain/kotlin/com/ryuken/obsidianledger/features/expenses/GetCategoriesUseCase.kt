package com.ryuken.obsidianledger.features.expenses

import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(
    private val repository: CategoryRepository
) {
    operator fun invoke(userId: String): Flow<List<Category>> {
        return repository.getCategories(userId)
    }
}
