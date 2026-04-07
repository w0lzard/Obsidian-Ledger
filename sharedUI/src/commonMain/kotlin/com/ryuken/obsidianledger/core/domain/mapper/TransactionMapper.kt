package com.ryuken.obsidianledger.core.domain.mapper

import com.ryuken.obsidianledger.core.database.TransactionEntity
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate

private fun displayNameForCategoryId(id: String): String =
    when (id) {
        "cat_food"      -> "Food & Dining"
        "cat_transport" -> "Transport"
        "cat_shopping"  -> "Shopping"
        "cat_health"    -> "Health"
        "cat_bills"     -> "Bills"
        "cat_housing"   -> "Housing"
        "cat_dining"    -> "Fine Dining"
        "cat_entertain" -> "Entertainment"
        "cat_savings"   -> "Savings"
        "cat_income"    -> "Income"
        else            -> id
    }

private fun emojiForCategoryId(id: String): String =
    when (id) {
        "cat_food"      -> "🍔"
        "cat_transport" -> "🚕"
        "cat_shopping"  -> "🛍"
        "cat_health"    -> "💊"
        "cat_bills"     -> "⚡"
        "cat_housing"   -> "🏠"
        "cat_dining"    -> "🍽"
        "cat_entertain" -> "🎬"
        "cat_savings"   -> "💰"
        "cat_income"    -> "💼"
        else            -> "💰"
    }

internal fun monthPrefix(year: Int, month: Int): String =
    "$year-${month.toString().padStart(2, '0')}"

internal fun TransactionEntity.toDomain(): Transaction {
    val cat = Category(
        id    = categoryId,
        name  = displayNameForCategoryId(categoryId),
        emoji = emojiForCategoryId(categoryId)
    )
    return Transaction(
        id        = id,
        amount    = amount,
        type      = TransactionType.valueOf(type),
        category  = cat,
        note      = note,
        date      = LocalDate.parse(date),
        createdAt = kotlinx.datetime.Instant.parse(createdAt),
        updatedAt = kotlinx.datetime.Instant.parse(updatedAt),
        isDirty   = isDirty == 1L,
        userId    = userId
    )
}
