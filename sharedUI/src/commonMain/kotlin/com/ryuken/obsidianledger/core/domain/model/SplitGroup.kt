package com.ryuken.obsidianledger.core.domain.model

import kotlin.time.Instant

data class SplitGroup(
    val id: String,
    val name: String,
    val members: List<GroupMember>,
    val createdBy: String,
    val createdAt: Instant
)

data class GroupMember(
    val id: String,
    val groupId: String,
    val userId: String?,
    val displayName: String,
    val email: String?
)
