package com.ryuken.obsidianledger.features.profile

import com.ryuken.obsidianledger.core.domain.helper.csvField
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository

class ExportCsvUseCase(
    private val transactionRepo: TransactionRepository
) {
    suspend operator fun invoke(userId: String): String {
        val transactions = transactionRepo.getAll(userId)

        val header = "Date,Type,Category,Amount,Note"
        val rows = transactions.joinToString("\n") { tx ->
            listOf(
                tx.date.toString(),
                tx.type.name,
                tx.category.name,
                tx.amount.toString(),
                tx.note ?: ""
            ).joinToString(",") { csvField(it) }
        }
        return if (transactions.isEmpty()) header else "$header\n$rows"
    }
}
