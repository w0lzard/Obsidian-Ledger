// AuthRepository.kt
package com.ryuken.obsidianledger.core.domain.repository

import kotlinx.coroutines.flow.Flow

// Distinct from NotAuthenticated so a cold-start session restore doesn't get
// misread as "signed out" and flash the login screen before storage finishes loading.
sealed interface AuthSessionState {
    data object Initializing : AuthSessionState
    data class Authenticated(val userId: String) : AuthSessionState
    data object NotAuthenticated : AuthSessionState
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(
        email       : String,
        password    : String,
        displayName : String
    )
    suspend fun signInWithGoogleIdToken(idToken: String, nonce: String)
    suspend fun signOut()
    suspend fun updateUser(displayName: String)
    suspend fun updatePassword(newPassword: String)
    fun currentUserId(): String?
    fun isSignedIn(): Boolean
    fun observeUserId(): Flow<String?>
    fun observeAuthState(): Flow<AuthSessionState>
    fun getSessionStatusString(): String
}
