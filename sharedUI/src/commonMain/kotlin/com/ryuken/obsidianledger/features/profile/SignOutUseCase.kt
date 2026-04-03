package com.ryuken.obsidianledger.features.profile

import com.ryuken.obsidianledger.core.domain.repository.AuthRepository

class SignOutUseCase(
    private val authRepo: AuthRepository
) {
    suspend operator fun invoke() {
        authRepo.signOut()
    }
}
