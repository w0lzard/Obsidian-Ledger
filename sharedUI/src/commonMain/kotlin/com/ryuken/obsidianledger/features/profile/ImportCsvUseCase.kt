package com.ryuken.obsidianledger.features.profile

import com.benasher44.uuid.uuid4
import com.ryuken.obsidianledger.core.domain.helper.parseCsvLine
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
class ImportCsvUseCase(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) {
    suspend operator fun invoke(userId: String, csv: String): ImportResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ImportResult(imported = 0, skipped = 0)

        val firstFieldIsDate = parseCsvLine(lines.first()).getOrNull(0)
            ?.let { runCatching { LocalDate.parse(it.trim()) }.isSuccess } ?: false
        val dataLines = if (firstFieldIsDate) lines else lines.drop(1)

        val categoriesByName = categoryRepo.observeAll(userId).first()
            .associateBy { it.name.lowercase() }
            .toMutableMap()

        var imported = 0
        var skipped = 0

        for (line in dataLines) {
            val fields = parseCsvLine(line)
            val date = fields.getOrNull(0)?.trim()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val type = fields.getOrNull(1)?.trim()?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            val categoryName = fields.getOrNull(2)?.trim()
            val amount = fields.getOrNull(3)?.trim()?.toDoubleOrNull()
            val note = fields.getOrNull(4)?.takeIf { it.isNotBlank() }

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

        return ImportResult(imported, skipped)
    }
}
