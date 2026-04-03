package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
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
import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.core.domain.mapper.monthPrefix
import com.ryuken.obsidianledger.core.domain.mapper.toDomain
import com.ryuken.obsidianledger.core.domain.dto.toDto
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class TransactionRepositoryImpl(
    private val db             : LedgerDatabase,
    private val supabaseClient : SupabaseClient
) : TransactionRepository {

    private val queries = db.transactionEntityQueries

    // ── Observe ───────────────────────────────────────────────────────
    override fun observeByMonth(
        userId : String,
        year   : Int,
        month  : Int
    ): Flow<List<Transaction>> {
        val prefix = monthPrefix(year, month)
        return queries
            .selectByMonth(userId = userId, monthPrefix = prefix)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
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
                val income = list
                    .filter { it.type == "INCOME" }
                    .sumOf { it.amount }
                val breakdown = list
                    .filter { it.type == "EXPENSE" }
                    .groupBy { it.categoryId }
                    .mapValues { (_, txs) -> txs.sumOf { it.amount } }
                MonthlySummary(
                    totalExpense      = expense,
                    totalIncome       = income,
                    categoryBreakdown = breakdown
                )
            }
    }

    // ── Write ─────────────────────────────────────────────────────────
    override suspend fun add(transaction: Transaction) {
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
                userId     = transaction.userId
            )
        }
    }

    override suspend fun update(transaction: Transaction) {
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
                userId     = transaction.userId
            )
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            queries.delete(id = id)
        }
    }


    override suspend fun syncPendingToRemote(userId: String) {
        withContext(Dispatchers.IO) {
            val dirty = queries.selectDirty().executeAsList()
            if (dirty.isEmpty()) return@withContext
            supabaseClient.postgrest["transactions"]
                .upsert(dirty.map { it.toDto() })
            dirty.forEach { queries.markClean(it.id) }
        }
    }
}






