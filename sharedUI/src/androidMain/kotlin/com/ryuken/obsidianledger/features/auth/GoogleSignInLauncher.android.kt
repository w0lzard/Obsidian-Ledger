package com.ryuken.obsidianledger.features.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.ryuken.obsidianledger.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

@Composable
actual fun rememberGoogleSignInLauncher(
    onResult: (Result<GoogleIdTokenResult>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return {
        scope.launch {
            try {
                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = MessageDigest.getInstance("SHA-256")
                    .digest(rawNonce.toByteArray())
                    .joinToString("") { "%02x".format(it) }

                val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val response = CredentialManager.create(context).getCredential(context, request)
                val credential = response.credential
                check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    "Unexpected credential type: ${credential.type}"
                }
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken

                onResult(Result.success(GoogleIdTokenResult(idToken = idToken, nonce = rawNonce)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}
