package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.database.BudgetEntity
import com.ryuken.obsidianledger.core.database.CategoryEntity
import com.ryuken.obsidianledger.core.domain.error.withRepositoryErrorHandling
import com.ryuken.obsidianledger.core.domain.helper.roundToCents
import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetPeriod
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import com.ryuken.obsidianledger.core.domain.dto.BudgetDto
import com.ryuken.obsidianledger.core.database.LedgerDatabase
import com.ryuken.obsidianledger.core.sync.LocalManifestRow
import com.ryuken.obsidianledger.core.sync.RemoteManifestRow
import com.ryuken.obsidianledger.core.sync.SyncMerger
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
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
                        // Read boundary: SUM() over REAL storage can carry float drift;
                        // round at the edge so percentUsed/status see exact cents.
                        spent       = row.spent.roundToCents(),
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
            val dirty = budgetQueries.selectDirty(userId = userId).executeAsList()
            if (dirty.isEmpty()) return@withContext

            val (tombstoned, live) = dirty.partition { it.deletedAt != null }
            if (live.isNotEmpty()) {
                supabaseClient.postgrest["budgets"].upsert(live.map { it.toDto() })
            }
            tombstoned.forEach {
                supabaseClient.postgrest["budgets"].delete { filter { eq("id", it.id) } }
            }
            // Rows are only marked clean after every remote operation above succeeded.
            dirty.forEach { budgetQueries.markClean(it.id) }
            budgetQueries.purgeTombstones()
        }
    }

    override suspend fun pullRemote(userId: String): Unit = withRepositoryErrorHandling("BudgetRepository.pullRemote") {
        withContext(Dispatchers.IO) {
            val remoteStamps = fetchRemoteStamps(userId)

            val localStamps = budgetQueries.selectManifest(userId = userId).executeAsList()
            val plan = SyncMerger.plan(
                local  = localStamps.map { LocalManifestRow(it.id, it.serverUpdatedAt, it.isDirty == 1L) },
                remote = remoteStamps.map { RemoteManifestRow(it.id, it.updatedAt) }
            )

            plan.toFetch.chunked(FETCH_CHUNK).forEach { chunk ->
                val rows = supabaseClient.postgrest["budgets"].select {
                    filter { eq("user_id", userId); isIn("id", chunk) }
                }.decodeList<BudgetDto>()
                db.transaction {
                    rows.forEach { dto ->
                        // Same guarded-apply pattern as transactions: never overwrite a
                        // locally dirty row with pulled remote state.
                        budgetQueries.insertRemote(
                            id              = dto.id,
                            categoryId      = dto.category_id,
                            limitAmount     = dto.limit_amount,
                            period          = dto.period,
                            isDirty         = 0L,
                            userId          = dto.user_id,
                            deletedAt       = null,
                            serverUpdatedAt = dto.updated_at
                        )
                        budgetQueries.applyRemote(
                            id              = dto.id,
                            categoryId      = dto.category_id,
                            limitAmount     = dto.limit_amount,
                            period          = dto.period,
                            serverUpdatedAt = dto.updated_at
                        )
                    }
                }
            }

            plan.toDeleteLocal.forEach { budgetQueries.deleteCleanById(it) }
        }
    }

    private suspend fun fetchRemoteStamps(userId: String): List<BudgetStampDto> {
        val stamps = mutableListOf<BudgetStampDto>()
        var offset = 0L
        while (true) {
            val page = supabaseClient.postgrest["budgets"].select(columns = Columns.list("id", "updated_at")) {
                filter { eq("user_id", userId) }
                range(offset, offset + MANIFEST_PAGE - 1)
            }.decodeList<BudgetStampDto>()
            stamps += page
            if (page.size < MANIFEST_PAGE) break
            offset += MANIFEST_PAGE
        }
        return stamps
    }

    private companion object {
        const val MANIFEST_PAGE = 1_000L
        const val FETCH_CHUNK   = 500
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

private fun BudgetEntity.toDto() =
    BudgetDto(
        id           = id,
        category_id  = categoryId,
        limit_amount = limitAmount,
        period       = period,
        user_id      = userId
    )

@Serializable
private data class BudgetStampDto(
    val id         : String,
    @SerialName("updated_at")
    val updatedAt  : String?
)
