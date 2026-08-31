package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.domain.error.withRepositoryErrorHandling
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import com.ryuken.obsidianledger.core.database.LedgerDatabase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.core.domain.mapper.monthPrefix
import com.ryuken.obsidianledger.core.domain.mapper.toDomain
import com.ryuken.obsidianledger.core.domain.dto.TransactionDto
import com.ryuken.obsidianledger.core.domain.dto.toDto
import com.ryuken.obsidianledger.core.domain.helper.roundToCents
import com.ryuken.obsidianledger.core.sync.LocalManifestRow
import com.ryuken.obsidianledger.core.sync.RemoteManifestRow
import com.ryuken.obsidianledger.core.sync.SyncMerger
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class TransactionRepositoryImpl(
    private val db               : LedgerDatabase,
    private val supabaseClient   : SupabaseClient,
    private val categoryRepository : CategoryRepository
) : TransactionRepository {

    private val queries = db.transactionEntityQueries

    // ── Observe ───────────────────────────────────────────────────────
    override fun observeByMonth(
        userId : String,
        year   : Int,
        month  : Int
    ): Flow<List<Transaction>> {
        val prefix = monthPrefix(year, month)
        return combine(
            queries.selectByMonth(userId = userId, monthPrefix = prefix).asFlow().mapToList(Dispatchers.IO),
            categoryRepository.observeAll(userId)
        ) { list, categories ->
            val categoriesById = categories.associateBy { it.id }
            list.map { it.toDomain(categoriesById) }
        }.catch { e ->
            Napier.e("observeByMonth failed, showing empty list", e)
            emit(emptyList())
        }
    }

    override suspend fun getAll(userId: String): List<Transaction> = withRepositoryErrorHandling("TransactionRepository.getAll") {
        withContext(Dispatchers.IO) {
            val categoriesById = categoryRepository.observeAll(userId).first().associateBy { it.id }
            queries.selectAll(userId = userId).executeAsList().map { it.toDomain(categoriesById) }
        }
    }

    override fun observeMonthlySummary(
        userId : String,
        year   : Int,
        month  : Int
    ): Flow<MonthlySummary> {
        val prefix = monthPrefix(year, month)
        return queries
            .selectByMonth(userId = userId, monthPrefix = prefix)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                val expense = list
                    .filter { it.type == "EXPENSE" }
                    .sumOf { it.amount }
                    .roundToCents()
                val income = list
                    .filter { it.type == "INCOME" }
                    .sumOf { it.amount }
                    .roundToCents()
                val breakdown = list
                    .filter { it.type == "EXPENSE" }
                    .groupBy { it.categoryId }
                    .mapValues { (_, txs) -> txs.sumOf { it.amount }.roundToCents() }
                MonthlySummary(
                    totalExpense      = expense,
                    totalIncome       = income,
                    categoryBreakdown = breakdown
                )
            }
            .catch { e ->
                Napier.e("observeMonthlySummary failed, showing empty summary", e)
                emit(MonthlySummary(totalExpense = 0.0, totalIncome = 0.0, categoryBreakdown = emptyMap()))
            }
    }

    // ── Write ─────────────────────────────────────────────────────────
    override suspend fun add(transaction: Transaction): Unit = withRepositoryErrorHandling("TransactionRepository.add") {
        withContext(Dispatchers.IO) {
            queries.insert(
                id         = transaction.id,
                amount     = transaction.amount,
                type       = transaction.type.name,
                categoryId = transaction.category.id,
                note       = transaction.note,
                date       = transaction.date.toString(),
                createdAt  = transaction.createdAt.toString(),
                updatedAt  = transaction.updatedAt.toString(),
                isDirty    = 1L,
                userId     = transaction.userId,
                deletedAt  = null
            )
        }
    }

    override suspend fun update(transaction: Transaction): Unit = withRepositoryErrorHandling("TransactionRepository.update") {
        withContext(Dispatchers.IO) {
            queries.insert(
                id         = transaction.id,
                amount     = transaction.amount,
                type       = transaction.type.name,
                categoryId = transaction.category.id,
                note       = transaction.note,
                date       = transaction.date.toString(),
                createdAt  = transaction.createdAt.toString(),
                updatedAt  = transaction.updatedAt.toString(),
                isDirty    = 1L,
                userId     = transaction.userId,
                deletedAt  = null
            )
        }
    }

    override suspend fun delete(id: String): Unit = withRepositoryErrorHandling("TransactionRepository.delete") {
        withContext(Dispatchers.IO) {
            queries.markDeleted(id = id, deletedAt = Clock.System.now().toString())
        }
    }

    override suspend fun syncPendingToRemote(userId: String): Unit = withRepositoryErrorHandling("TransactionRepository.syncPendingToRemote") {
        withContext(Dispatchers.IO) {
            val dirty = queries.selectDirty(userId = userId).executeAsList()
            if (dirty.isEmpty()) return@withContext

            val (tombstoned, live) = dirty.partition { it.deletedAt != null }
            if (live.isNotEmpty()) {
                supabaseClient.postgrest["transactions"].upsert(live.map { it.toDto() })
            }
            tombstoned.forEach {
                supabaseClient.postgrest["transactions"].delete { filter { eq("id", it.id) } }
            }
            // Rows are only marked clean after every remote operation above succeeded —
            // a failure mid-push throws and leaves all rows dirty for the next run.
            dirty.forEach { queries.markClean(it.id) }
            queries.purgeTombstones()
        }
    }

    override suspend fun pullRemote(userId: String): Unit = withRepositoryErrorHandling("TransactionRepository.pullRemote") {
        withContext(Dispatchers.IO) {
            // 1. Manifest: (id, updated_at) for every remote row of this user.
            //    Paginated — PostgREST servers cap rows per request (Supabase default 1000).
            val remoteStamps = fetchRemoteStamps(userId)

            // 2. Diff against the local cache. SyncMerger owns the conflict policy:
            //    dirty locals are untouchable, clean locals missing remotely are deleted,
            //    changed/new remote rows are fetched back in full.
            val localStamps = queries.selectManifest(userId = userId).executeAsList()
            val plan = SyncMerger.plan(
                local  = localStamps.map { LocalManifestRow(it.id, it.serverUpdatedAt, it.isDirty == 1L) },
                remote = remoteStamps.map { RemoteManifestRow(it.id, it.updatedAt) }
            )

            // 3. Fetch full rows for new/changed ids (chunks stay under the row cap).
            plan.toFetch.chunked(FETCH_CHUNK).forEach { chunk ->
                val rows = supabaseClient.postgrest["transactions"].select {
                    filter { eq("user_id", userId); isIn("id", chunk) }
                }.decodeList<TransactionDto>()
                db.transaction {
                    rows.forEach { dto ->
                        // INSERT OR IGNORE seeds the row if absent; the guarded UPDATE
                        // applies remote state only when the row is not locally dirty
                        // (a concurrent local edit between push and pull must survive).
                        queries.insertRemote(
                            id              = dto.id,
                            amount          = dto.amount,
                            type            = dto.type,
                            categoryId      = dto.categoryId,
                            note            = dto.note,
                            date            = dto.date,
                            createdAt       = dto.createdAt,
                            updatedAt       = dto.updatedAt,
                            isDirty         = 0L,
                            userId          = dto.userId,
                            deletedAt       = null,
                            serverUpdatedAt = dto.updatedAt
                        )
                        queries.applyRemote(
                            id              = dto.id,
                            amount          = dto.amount,
                            type            = dto.type,
                            categoryId      = dto.categoryId,
                            note            = dto.note,
                            date            = dto.date,
                            createdAt       = dto.createdAt,
                            updatedAt       = dto.updatedAt,
                            serverUpdatedAt = dto.updatedAt
                        )
                    }
                }
            }

            // 4. Propagate remote deletions to clean local rows. Dirty rows missing
            //    remotely are deliberately kept — they resurrect on the next push.
            plan.toDeleteLocal.forEach { queries.deleteCleanById(it) }
        }
    }

    private suspend fun fetchRemoteStamps(userId: String): List<TransactionStampDto> {
        val stamps = mutableListOf<TransactionStampDto>()
        var offset = 0L
        while (true) {
            val page = supabaseClient.postgrest["transactions"].select(columns = Columns.list("id", "updated_at")) {
                filter { eq("user_id", userId) }
                range(offset, offset + MANIFEST_PAGE - 1)
            }.decodeList<TransactionStampDto>()
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

@Serializable
internal data class TransactionStampDto(
    val id         : String,
    @SerialName("updated_at")
    val updatedAt  : String?
)
