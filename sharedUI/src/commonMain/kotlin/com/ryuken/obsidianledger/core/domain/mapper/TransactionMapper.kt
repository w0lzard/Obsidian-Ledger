package com.ryuken.obsidianledger.core.domain.mapper

import com.ryuken.obsidianledger.core.database.TransactionEntity
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate

internal fun monthPrefix(year: Int, month: Int): String =
    "$year-${month.toString().padStart(2, '0')}"

internal fun TransactionEntity.toDomain(): Transaction {
    val cat = Category(
        id    = categoryId,
        name  = categoryId,   // resolved properly via CategoryRepository
        emoji = ""
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
