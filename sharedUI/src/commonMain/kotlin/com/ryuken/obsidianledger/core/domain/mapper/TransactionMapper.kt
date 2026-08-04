package com.ryuken.obsidianledger.core.domain.mapper

import com.ryuken.obsidianledger.core.database.TransactionEntity
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate

internal fun monthPrefix(year: Int, month: Int): String =
    "$year-${month.toString().padStart(2, '0')}"

// Fallback only for a category deleted after the transaction was created — real
// name/emoji come from categoriesById (backed by CategoryRepositoryImpl).
private fun unknownCategory(id: String) = Category(id = id, name = id, emoji = "💰")

internal fun TransactionEntity.toDomain(categoriesById: Map<String, Category>): Transaction {
    val cat = categoriesById[categoryId] ?: unknownCategory(categoryId)
    return Transaction(
        id        = id,
        amount    = amount,
        type      = TransactionType.valueOf(type),
        category  = cat,
        note      = note,
        date      = LocalDate.parse(date),
        createdAt = kotlin.time.Instant.parse(createdAt),
        updatedAt = kotlin.time.Instant.parse(updatedAt),
        isDirty   = isDirty == 1L,
        userId    = userId
    )
}
