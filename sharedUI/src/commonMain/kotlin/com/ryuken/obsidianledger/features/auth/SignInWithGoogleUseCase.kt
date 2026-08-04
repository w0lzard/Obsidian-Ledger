package com.ryuken.obsidianledger.features.auth

import com.ryuken.obsidianledger.core.domain.repository.AuthRepository

class SignInWithGoogleUseCase(
    private val authRepo: AuthRepository
) {
    suspend operator fun invoke(idToken: String, nonce: String) {
        authRepo.signInWithGoogleIdToken(idToken, nonce)
    }
}
