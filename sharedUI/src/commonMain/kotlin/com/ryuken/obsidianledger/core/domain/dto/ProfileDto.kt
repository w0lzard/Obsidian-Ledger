package com.ryuken.obsidianledger.core.domain.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id           : String,
    val display_name : String,
    val email        : String,
    @SerialName("created_at")
    val created_at   : String? = null
)