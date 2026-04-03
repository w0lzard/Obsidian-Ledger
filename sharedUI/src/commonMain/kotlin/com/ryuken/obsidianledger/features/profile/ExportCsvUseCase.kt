package com.ryuken.obsidianledger.features.profile

import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class ExportCsvUseCase(
    private val transactionRepo: TransactionRepository
) {
    suspend operator fun invoke(userId: String): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val transactions = transactionRepo
            .observeByMonth(userId, today.year, today.monthNumber)
            .first()

        val header = "Date,Type,Category,Amount,Note"
        val rows = transactions.joinToString("\n") { tx ->
            "${tx.date},${tx.type.name},${tx.category.name},${tx.amount},${tx.note ?: ""}"
        }
        return "$header\n$rows"
    }
}
