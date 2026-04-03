package com.ryuken.obsidianledger.features.auth

import com.ryuken.obsidianledger.core.domain.repository.AuthRepository

class SignInUseCase(
    private val authRepo: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) {
        authRepo.signIn(email, password)
    }
}
