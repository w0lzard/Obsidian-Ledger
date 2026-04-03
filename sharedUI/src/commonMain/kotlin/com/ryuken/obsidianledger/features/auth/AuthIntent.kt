package com.ryuken.obsidianledger.features.auth

sealed interface AuthIntent {
    data class TabChanged(val tab: AuthTab)           : AuthIntent
    data class EmailChanged(val email: String)        : AuthIntent
    data class PasswordChanged(val password: String)  : AuthIntent
    data class DisplayNameChanged(val name: String)   : AuthIntent
    data object SubmitClick                           : AuthIntent
}
