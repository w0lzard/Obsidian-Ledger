package com.ryuken.obsidianledger.core.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class Transaction(
    val id : String,
    val amount : Double,
    val type : TransactionType,
    val category : Category,
    val note : String?,
    val date : LocalDate,
    val createdAt : Instant,
    val updatedAt : Instant,
    val isDirty : Boolean = true,
    val userId : String
)
