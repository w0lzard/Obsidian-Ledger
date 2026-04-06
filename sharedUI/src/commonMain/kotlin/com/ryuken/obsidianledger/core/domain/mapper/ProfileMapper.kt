package com.ryuken.obsidianledger.core.domain.mapper

import com.ryuken.obsidianledger.core.domain.dto.ProfileDto
import com.ryuken.obsidianledger.core.domain.model.UserProfile

fun ProfileDto.toDomain() = UserProfile(
    id          = id,
    displayName = display_name,
    email       = email,
    createdAt   = created_at
)