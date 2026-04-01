package com.ryuken.obsidianledger.core.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: String,
    val category_id: String,
    val limit_amount: Double,
    val period: String,
    val user_id: String
)
