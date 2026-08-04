package com.ryuken.obsidianledger.core.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class Settlement(
    val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amount: Double,
    val date: LocalDate,
    val createdAt: Instant
)
