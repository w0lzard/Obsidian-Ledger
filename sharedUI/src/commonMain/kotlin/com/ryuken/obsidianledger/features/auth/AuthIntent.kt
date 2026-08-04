package com.ryuken.obsidianledger.features.auth

sealed interface AuthIntent {
    data class TabChanged(val tab: AuthTab)           : AuthIntent
    data class EmailChanged(val email: String)        : AuthIntent
    data class PasswordChanged(val password: String)  : AuthIntent
    data class DisplayNameChanged(val name: String)   : AuthIntent
    data object SubmitClick                           : AuthIntent
    data class GoogleIdTokenReceived(val idToken: String, val nonce: String) : AuthIntent
    data class GoogleSignInFailed(val message: String) : AuthIntent
}
