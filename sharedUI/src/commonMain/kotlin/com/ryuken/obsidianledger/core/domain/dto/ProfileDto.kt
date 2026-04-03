package com.ryuken.obsidianledger.core.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id           : String,
    val display_name : String,
    val email        : String
)