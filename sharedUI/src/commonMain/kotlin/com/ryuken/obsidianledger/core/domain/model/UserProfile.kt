package com.ryuken.obsidianledger.core.domain.model

data class UserProfile(
    val id          : String,
    val displayName : String,
    val email       : String,
    val createdAt   : String? = null
)
