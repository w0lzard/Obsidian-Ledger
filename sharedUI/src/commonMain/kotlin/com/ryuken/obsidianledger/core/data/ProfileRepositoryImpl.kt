package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.domain.model.UserProfile
import com.ryuken.obsidianledger.core.domain.repository.ProfileRepository
import com.ryuken.obsidianledger.core.domain.dto.ProfileDto
import com.ryuken.obsidianledger.core.domain.mapper.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import io.github.aakira.napier.Napier

class ProfileRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : ProfileRepository {

    override suspend fun getProfile(userId: String): UserProfile {
        Napier.d("ProfileRepository: fetching profile for userId = $userId")
        return try {
            val result = supabaseClient.postgrest["profiles"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<ProfileDto>()
                .toDomain()
            Napier.d("ProfileRepository: profile fetched — displayName = ${result.displayName}")
            result
        } catch (e: Exception) {
            Napier.e("ProfileRepository: fetch failed — ${e.message}", e)
            throw e
        }
    }

    @Serializable
    private data class ProfileUpdateDto(
        @SerialName("display_name") val displayName: String
    )

    override suspend fun updateProfile(userId: String, displayName: String) {
        supabaseClient.postgrest["profiles"]
            .update(ProfileUpdateDto(displayName)) { filter { eq("id", userId) } }
    }

    override fun observeProfile(userId: String): Flow<UserProfile> = flow {
        emit(getProfile(userId))
    }
}