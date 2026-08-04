package com.ryuken.obsidianledger.core.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class SplitExpense(
    val id: String,
    val groupId: String,
    val description: String,
    val amount: Double,
    val paidByMemberId: String,
    val date: LocalDate,
    val shares: List<SplitShare>,
    val createdAt: Instant
)

data class SplitShare(
    val memberId: String,
    val amount: Double
)
