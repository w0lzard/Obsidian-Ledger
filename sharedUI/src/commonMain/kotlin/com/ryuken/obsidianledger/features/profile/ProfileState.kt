package com.ryuken.obsidianledger.features.profile

import com.ryuken.obsidianledger.core.domain.model.UserProfile

data class ProfileState(
    val profile          : UserProfile? = null,
    val transactionCount : Int          = 0,
    val activeBudgets    : Int          = 0,
    val memberSince      : String       = "—",
    val lastSyncTimestamp : String       = "Never",
    val isLoading        : Boolean      = true,
    val error            : String?      = null
) {
    val initials: String
        get() = profile?.displayName
            ?.split(" ")
            ?.mapNotNull { it.firstOrNull()?.uppercase() }
            ?.take(2)
            ?.joinToString("")
            ?: "?"
}
