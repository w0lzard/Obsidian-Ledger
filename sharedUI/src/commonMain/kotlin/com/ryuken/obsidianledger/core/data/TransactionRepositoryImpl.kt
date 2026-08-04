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
import com.ryuken.obsidianledger.core.domain.dto.toDto
import com.ryuken.obsidianledger.core.domain.helper.roundToCents
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
            val dirty = queries.selectDirty().executeAsList()
            if (dirty.isEmpty()) return@withContext

            val (tombstoned, live) = dirty.partition { it.deletedAt != null }
            if (live.isNotEmpty()) {
                supabaseClient.postgrest["transactions"].upsert(live.map { it.toDto() })
            }
            tombstoned.forEach {
                supabaseClient.postgrest["transactions"].delete { filter { eq("id", it.id) } }
            }
            dirty.forEach { queries.markClean(it.id) }
            queries.purgeTombstones()
        }
    }
}
