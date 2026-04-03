package com.ryuken.obsidianledger.core.domain.repository

import com.ryuken.obsidianledger.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun getProfile(userId: String): UserProfile
    fun observeProfile(userId: String): Flow<UserProfile>
}