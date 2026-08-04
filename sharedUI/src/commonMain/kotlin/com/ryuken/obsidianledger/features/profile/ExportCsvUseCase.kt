package com.ryuken.obsidianledger.features.profile

import com.ryuken.obsidianledger.core.domain.helper.csvField
import com.ryuken.obsidianledger.core.domain.helper.sanitizeCsvFormulaInjection
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
                // Category name and note are free text (user-typed, or from an imported
                // CSV) — sanitize before category/amount stay as-is since a date, enum
                // name, and numeric amount can never start with a formula-trigger char.
                sanitizeCsvFormulaInjection(tx.category.name),
                tx.amount.toString(),
                sanitizeCsvFormulaInjection(tx.note ?: "")
            ).joinToString(",") { csvField(it) }
        }
        return if (transactions.isEmpty()) header else "$header\n$rows"
    }
}
