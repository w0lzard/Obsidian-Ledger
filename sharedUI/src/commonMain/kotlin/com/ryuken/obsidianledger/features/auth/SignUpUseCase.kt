package com.ryuken.obsidianledger.features.auth

import com.ryuken.obsidianledger.core.domain.repository.AuthRepository

class SignUpUseCase(
    private val authRepo: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, displayName: String) {
        authRepo.signUp(email, password, displayName)
    }
}
