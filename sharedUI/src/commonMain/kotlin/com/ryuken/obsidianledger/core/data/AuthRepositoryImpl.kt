package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
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
                    put("display_name", displayName)
                }
            }
        }
    }

    override suspend fun signInWithGoogle(redirectTo: String) {
        withContext(Dispatchers.IO) {
            supabaseClient.auth.signInWith(Google, redirectUrl = redirectTo) {
                scopes.add("email")
                scopes.add("profile")
            }
        }
    }

    override suspend fun signOut() =
        withContext(Dispatchers.IO) {
            supabaseClient.auth.signOut()
        }

    override suspend fun updateUser(displayName: String) {
        withContext(Dispatchers.IO) {
            supabaseClient.auth.updateUser {
                data = buildJsonObject {
                    put("display_name", displayName)
                }
            }
        }
    }

    override suspend fun updatePassword(newPassword: String) {
        withContext(Dispatchers.IO) {
            supabaseClient.auth.updateUser {
                password = newPassword
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun extractUserIdFromJwt(token: String?): String? {
        if (token == null) return null
        return try {
            val parts = token.split(".")
            if (parts.size == 3) {
                // Pad to multiple of 4 if needed
                val payload = parts[1].padEnd(parts[1].length + (4 - parts[1].length % 4) % 4, '=')
                val decoded = Base64.UrlSafe.decode(payload).decodeToString()
                val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(decoded).jsonObject
                json["sub"]?.jsonPrimitive?.content
            } else null
        } catch (e: Exception) {
            Napier.e("Failed to decode JWT to extract userId", e)
            null
        }
    }

    override fun currentUserId(): String? {
        val auth = supabaseClient.auth
        val userId = auth.currentUserOrNull()?.id 
            ?: extractUserIdFromJwt(auth.currentSessionOrNull()?.accessToken)
        Napier.d("AuthRepository.currentUserId: $userId")
        return userId
    }

    override fun isSignedIn(): Boolean =
        supabaseClient.auth.currentSessionOrNull() != null

    override fun getSessionStatusString(): String =
        supabaseClient.auth.sessionStatus.value.toString()

    override fun observeUserId(): Flow<String?> =
        supabaseClient.auth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val id = status.session.user?.id ?: extractUserIdFromJwt(status.session.accessToken)
                        Napier.d("AuthRepository: session authenticated, userId = $id")
                        id
                    }
                    is SessionStatus.Initializing -> {
                        Napier.d("AuthRepository: initializing session (loading from storage)...")
                        null
                    }
                    is SessionStatus.NotAuthenticated -> {
                        Napier.d("AuthRepository: not authenticated")
                        null
                    }
                    else -> {
                        Napier.e("AuthRepository: other session status: $status")
                        null
                    }
                }
            }
            .distinctUntilChanged()
}