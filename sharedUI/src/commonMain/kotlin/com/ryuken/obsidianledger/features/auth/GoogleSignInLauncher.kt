package com.ryuken.obsidianledger.features.auth

import androidx.compose.runtime.Composable

data class GoogleIdTokenResult(val idToken: String, val nonce: String)

/**
 * Returns a trigger function that starts the platform's native Google sign-in flow.
 * [onResult] fires once with the ID token (plus the raw nonce, for Supabase's IDToken
 * verification) on success, or the failure exception otherwise.
 */
@Composable
expect fun rememberGoogleSignInLauncher(
    onResult: (Result<GoogleIdTokenResult>) -> Unit
): () -> Unit
