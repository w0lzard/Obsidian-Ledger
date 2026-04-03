package com.ryuken.obsidianledger.features.dashboard

import com.ryuken.obsidianledger.core.domain.model.UserProfile
import com.ryuken.obsidianledger.core.domain.repository.ProfileRepository

class GetProfileUseCase(
    private val profileRepo: ProfileRepository
) {
    suspend operator fun invoke(userId: String): UserProfile {
        return profileRepo.getProfile(userId)
    }
}
