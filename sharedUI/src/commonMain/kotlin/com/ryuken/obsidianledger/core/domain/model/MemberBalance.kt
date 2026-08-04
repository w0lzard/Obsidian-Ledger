package com.ryuken.obsidianledger.core.domain.model

data class MemberBalance(
    val memberId: String,
    val displayName: String,
    val email: String?,
    // Positive: this member is owed money. Negative: this member owes money.
    val netAmount: Double
)
