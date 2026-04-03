package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    override suspend fun signIn(
        email    : String,
        password : String
    ) = withContext(Dispatchers.IO) {
        supabaseClient.auth.signInWith(Email) {
            this.email    = email
            this.password = password
        }
    }

    override suspend fun signUp(
        email       : String,
        password    : String,
        displayName : String
    ) {
        withContext(Dispatchers.IO) {
            supabaseClient.auth.signUpWith(Email) {
                this.email    = email
                this.password = password
                this.data     = buildJsonObject {
                    // Supabase trigger reads this to create the profiles row
                    put("display_name", displayName)
                }
            }
        }
    }

    override suspend fun signOut() =
        withContext(Dispatchers.IO) {
            supabaseClient.auth.signOut()
        }

    override fun currentUserId(): String? =
        supabaseClient.auth.currentUserOrNull()?.id

    override fun isSignedIn(): Boolean =
        supabaseClient.auth.currentSessionOrNull() != null
}