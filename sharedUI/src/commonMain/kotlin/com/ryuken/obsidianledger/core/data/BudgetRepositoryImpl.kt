package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.database.BudgetEntity
import com.ryuken.obsidianledger.core.database.CategoryEntity
import com.ryuken.obsidianledger.core.domain.error.withRepositoryErrorHandling
import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetPeriod
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import com.ryuken.obsidianledger.core.database.LedgerDatabase
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

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
            .catch { e ->
                Napier.e("observeBudgetsWithSpending failed, showing empty list", e)
                emit(emptyList())
            }
    }

    // ── Write ─────────────────────────────────────────────────────────
    override suspend fun add(budget: Budget): Unit = withRepositoryErrorHandling("BudgetRepository.add") {
        withContext(Dispatchers.IO) {
            budgetQueries.insert(
                id          = budget.id,
                categoryId  = budget.category.id,
                limitAmount = budget.limitAmount,
                period      = budget.period.name,
                isDirty     = 1L,
                userId      = budget.userId,
                deletedAt   = null
            )
        }
    }

    override suspend fun delete(id: String): Unit = withRepositoryErrorHandling("BudgetRepository.delete") {
        withContext(Dispatchers.IO) {
            budgetQueries.markDeleted(id = id, deletedAt = Clock.System.now().toString())
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────
    override suspend fun syncPendingToRemote(userId: String): Unit = withRepositoryErrorHandling("BudgetRepository.syncPendingToRemote") {
        withContext(Dispatchers.IO) {
            val dirty = budgetQueries.selectDirty().executeAsList()
            if (dirty.isEmpty()) return@withContext

            val (tombstoned, live) = dirty.partition { it.deletedAt != null }
            if (live.isNotEmpty()) {
                supabaseClient.postgrest["budgets"].upsert(live.map { it.toDto() })
            }
            tombstoned.forEach {
                supabaseClient.postgrest["budgets"].delete { filter { eq("id", it.id) } }
            }
            dirty.forEach { budgetQueries.markClean(it.id) }
            budgetQueries.purgeTombstones()
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
