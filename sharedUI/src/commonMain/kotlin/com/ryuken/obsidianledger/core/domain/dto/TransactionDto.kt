package com.ryuken.obsidianledger.core.domain.dto

import com.ryuken.obsidianledger.core.database.TransactionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TransactionDto(
    val id          : String,
    val amount      : Double,
    val type        : String,
    @SerialName("category_id")
    val categoryId  : String,
    val note        : String?,
    val date        : String,
    @SerialName("created_at")
    val createdAt   : String,
    @SerialName("updated_at")
    val updatedAt   : String,
    @SerialName("user_id")
    val userId      : String
)
internal fun TransactionEntity.toDto() =
    TransactionDto(
        id         = id,
        amount     = amount,
        type       = type,
        categoryId = categoryId,
        note       = note,
        date       = date,
        createdAt  = createdAt,
        updatedAt  = updatedAt,
        userId     = userId
    )