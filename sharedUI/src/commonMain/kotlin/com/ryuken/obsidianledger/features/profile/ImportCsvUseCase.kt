package com.ryuken.obsidianledger.features.profile

import com.benasher44.uuid.uuid4
import com.ryuken.obsidianledger.core.domain.helper.parseCsv
import com.ryuken.obsidianledger.core.domain.helper.roundToCents
import com.ryuken.obsidianledger.core.domain.helper.unsanitizeCsvFormulaInjection
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

data class ImportResult(val imported: Int, val skipped: Int)

// Expects the same header ExportCsvUseCase writes: Date,Type,Category,Amount,Note
// (header row is optional — detected by checking whether the first field parses as a date).
// Parses the whole document with an RFC4180 parser, so quoted fields containing
// commas, escaped quotes or newlines round-trip correctly.
class ImportCsvUseCase(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) {
    suspend operator fun invoke(userId: String, csv: String): ImportResult {
        val rows = parseCsv(csv)
        if (rows.isEmpty()) return ImportResult(imported = 0, skipped = 0)

        val firstFieldIsDate = rows.first().getOrNull(0)
            ?.let { runCatching { LocalDate.parse(it.trim()) }.isSuccess } ?: false
        val dataRows = if (firstFieldIsDate) rows else rows.drop(1)

        val categoriesByName = categoryRepo.observeAll(userId).first()
            .associateBy { it.name.lowercase() }
            .toMutableMap()

        var imported = 0
        var skipped = 0

        for (fields in dataRows) {
            val date = fields.getOrNull(0)?.trim()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val type = fields.getOrNull(1)?.trim()?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            val categoryName = fields.getOrNull(2)?.trim()?.let { unsanitizeCsvFormulaInjection(it) }
            // Domain rule: sign is conveyed by TransactionType, so the amount itself must
            // be strictly positive — a zero or negative value is malformed data, skipped.
            val amount = fields.getOrNull(3)?.trim()?.toDoubleOrNull()
                ?.takeIf { it > 0.0 }
                ?.roundToCents()
            val note = fields.getOrNull(4)?.let { unsanitizeCsvFormulaInjection(it) }?.takeIf { it.isNotBlank() }

            if (date == null || type == null || categoryName.isNullOrBlank() || amount == null) {
                skipped++
                continue
            }

            val category = categoriesByName.getOrPut(categoryName.lowercase()) {
                val newCategory = Category(id = uuid4().toString(), name = categoryName, emoji = "💰", isCustom = true)
                categoryRepo.insertCustom(newCategory, userId)
                newCategory
            }

            val now = Clock.System.now()
            transactionRepo.add(
                Transaction(
                    id        = uuid4().toString(),
                    amount    = amount,
                    type      = type,
                    category  = category,
                    note      = note,
                    date      = date,
                    createdAt = now,
                    updatedAt = now,
                    isDirty   = true,
                    userId    = userId
                )
            )
            imported++
        }

        return ImportResult(imported = imported, skipped = skipped)
    }
}
