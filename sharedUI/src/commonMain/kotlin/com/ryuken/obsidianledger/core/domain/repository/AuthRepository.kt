// AuthRepository.kt
package com.ryuken.obsidianledger.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(
        email       : String,
        password    : String,
        displayName : String
    )
    suspend fun signInWithGoogle(redirectTo: String)
    suspend fun signOut()
    suspend fun updateUser(displayName: String)
    suspend fun updatePassword(newPassword: String)
    fun currentUserId(): String?
    fun isSignedIn(): Boolean
    fun observeUserId(): Flow<String?>
    fun getSessionStatusString(): String
}