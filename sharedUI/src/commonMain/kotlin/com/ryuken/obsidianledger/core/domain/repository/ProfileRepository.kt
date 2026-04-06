package com.ryuken.obsidianledger.core.domain.repository

import com.ryuken.obsidianledger.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun getProfile(userId: String): UserProfile
    suspend fun updateProfile(userId: String, displayName: String)
    fun observeProfile(userId: String): Flow<UserProfile>
}