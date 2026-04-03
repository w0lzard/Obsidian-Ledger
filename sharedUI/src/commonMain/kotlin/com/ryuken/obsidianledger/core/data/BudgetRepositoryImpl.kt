package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.database.BudgetEntity
import com.ryuken.obsidianledger.core.database.CategoryEntity
import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetPeriod
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import com.ryuken.obsidianledger.core.database.LedgerDatabase
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class BudgetRepositoryImpl(
    private val db              : LedgerDatabase,
    private val supabaseClient  : SupabaseClient
) : BudgetRepository {

    private val budgetQueries   = db.budgetEntityQueries
    private val categoryQueries = db.categoryEntityQueries

    // ── Observe ───────────────────────────────────────────────────────
    override fun observeBudgetsWithSpending(
        userId : String,
        year   : Int,
        month  : Int
    ): Flow<List<Budget>> {
        val prefix = "$year-${month.toString().padStart(2, '0')}"
        return budgetQueries
            .budgetWithSpending(userId = userId, monthPrefix = prefix)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { row ->
                    val cat = categoryQueries
                        .selectById(row.categoryId)
                        .executeAsOneOrNull()
                        ?.toDomain()
                        ?: Category(
                            id    = row.categoryId,
                            name  = row.categoryId,
                            emoji = ""
                        )
                    Budget(
                        id          = row.id,
                        category    = cat,
                        limitAmount = row.limitAmount,
                        spent       = row.spent,
                        period      = BudgetPeriod.valueOf(row.period),
                        userId      = row.userId,
                        isDirty     = row.isDirty == 1L
                    )
                }
            }
    }

    // ── Write ─────────────────────────────────────────────────────────
    override suspend fun add(budget: Budget) {
        withContext(Dispatchers.IO) {
            budgetQueries.insert(
                id          = budget.id,
                categoryId  = budget.category.id,
                limitAmount = budget.limitAmount,
                period      = budget.period.name,
                isDirty     = 1L,
                userId      = budget.userId
            )
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            budgetQueries.delete(id = id)
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────
    override suspend fun syncPendingToRemote(userId: String) {
        withContext(Dispatchers.IO) {
            val dirty = budgetQueries.selectAll(userId).executeAsList()
                .filter { it.isDirty == 1L }
            if (dirty.isEmpty()) return@withContext
            supabaseClient.postgrest["budgets"]
                .upsert(dirty.map { it.toDto() })
            dirty.forEach {
                budgetQueries.insert(
                    id          = it.id,
                    categoryId  = it.categoryId,
                    limitAmount = it.limitAmount,
                    period      = it.period,
                    isDirty     = 0L,
                    userId      = it.userId
                )
            }
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────

private fun CategoryEntity.toDomain() =
    Category(
        id       = id,
        name     = name,
        emoji    = emoji,
        colorHex = colorHex,
        isCustom = isCustom == 1L
    )

@Serializable
private data class BudgetDto(
    val id           : String,
    @SerialName("category_id")
    val categoryId   : String,
    @SerialName("limit_amount")
    val limitAmount  : Double,
    val period       : String,
    @SerialName("user_id")
    val userId       : String
)

private fun BudgetEntity.toDto() =
    BudgetDto(
        id          = id,
        categoryId  = categoryId,
        limitAmount = limitAmount,
        period      = period,
        userId      = userId
    )