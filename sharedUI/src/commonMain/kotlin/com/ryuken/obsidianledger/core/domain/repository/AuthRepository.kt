// AuthRepository.kt
package com.ryuken.obsidianledger.core.domain.repository

interface AuthRepository {
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String, displayName: String)
    suspend fun signOut()
    fun currentUserId(): String?
    fun isSignedIn(): Boolean
}