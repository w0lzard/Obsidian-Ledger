package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.database.CategoryEntity
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.database.LedgerDatabase
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CategoryRepositoryImpl(
    private val db: LedgerDatabase
) : CategoryRepository {

    private val queries = db.categoryEntityQueries

    override fun observeAll(userId: String): Flow<List<Category>> =
        queries
            .selectAll(userId = userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun insertCustom(
        category : Category,
        userId   : String
    ) {
        withContext(Dispatchers.IO) {
            queries.insertCustom(
                id        = category.id,
                name      = category.name,
                emoji     = category.emoji,
                colorHex  = category.colorHex,
                userId    = userId,
                sortOrder = 99L
            )
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            queries.delete(id = id)
        }
    }

    override suspend fun getDefaultCategory(id: String): Category =
        withContext(Dispatchers.IO) {
            queries.selectById(id).executeAsOneOrNull()?.toDomain()
                ?: Category(
                    id       = "cat_food",
                    name     = "General",
                    emoji    = "💰",
                    colorHex = "#00C896",
                    isCustom = false
                )
        }

    // ── Seed defaults on first launch ─────────────────────────────────
    suspend fun seedDefaultsIfEmpty(userId: String) =
        withContext(Dispatchers.IO) {
            val existing = queries.selectDefaults().executeAsList()
            if (existing.isNotEmpty()) return@withContext
            DefaultCategories.all.forEach { cat ->
                queries.insertCustom(
                    id        = cat.id,
                    name      = cat.name,
                    emoji     = cat.emoji,
                    colorHex  = cat.colorHex,
                    userId    = null,
                    sortOrder = cat.sortOrder.toLong()
                )
            }
        }
}

// ── Mapper ────────────────────────────────────────────────────────────

private fun CategoryEntity.toDomain() =
    Category(
        id       = id,
        name     = name,
        emoji    = emoji,
        colorHex = colorHex,
        isCustom = isCustom == 1L
    )

// ── Default categories ────────────────────────────────────────────────

private data class DefaultCategory(
    val id        : String,
    val name      : String,
    val emoji     : String,
    val colorHex  : String,
    val sortOrder : Int
)

private object DefaultCategories {
    val all = listOf(
        DefaultCategory("cat_food",      "Food & Dining",  "🍔", "#FF6B6B", 0),
        DefaultCategory("cat_transport", "Transport",      "🚕", "#4ECDC4", 1),
        DefaultCategory("cat_shopping",  "Shopping",       "🛍", "#45B7D1", 2),
        DefaultCategory("cat_health",    "Health",         "💊", "#96CEB4", 3),
        DefaultCategory("cat_bills",     "Bills",          "⚡", "#FFEAA7", 4),
        DefaultCategory("cat_housing",   "Housing",        "🏠", "#DDA0DD", 5),
        DefaultCategory("cat_dining",    "Fine Dining",    "🍽", "#F0E68C", 6),
        DefaultCategory("cat_entertain", "Entertainment",  "🎬", "#98FB98", 7),
        DefaultCategory("cat_savings",   "Savings",        "💰", "#87CEEB", 8),
        DefaultCategory("cat_income",    "Income",         "💼", "#00C896", 9)
    )
}