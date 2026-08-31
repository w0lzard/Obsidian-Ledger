package com.ryuken.obsidianledger.core.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: String,
    val category_id: String,
    val limit_amount: Double,
    val period: String,
    val user_id: String,
    // Server-maintained timestamps. Defaults keep them OUT of push payloads
    // (kotlinx omits fields equal to their default) while still decoding
    // them on pull, where they double as the change-detection stamp.
    val created_at: String? = null,
    val updated_at: String? = null
)
