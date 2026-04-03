package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.domain.model.UserProfile
import com.ryuken.obsidianledger.core.domain.repository.ProfileRepository
import com.ryuken.obsidianledger.core.domain.dto.ProfileDto
import com.ryuken.obsidianledger.core.domain.mapper.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ProfileRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : ProfileRepository {

    override suspend fun getProfile(userId: String): UserProfile =
        supabaseClient.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<ProfileDto>()
            .toDomain()

    override fun observeProfile(userId: String): Flow<UserProfile> = flow {
        emit(getProfile(userId))
    }
}