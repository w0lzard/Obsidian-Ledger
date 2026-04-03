package com.ryuken.obsidianledger.features.auth

sealed interface AuthEffect {
    data object AuthSuccess                   : AuthEffect
    data class Error(val message: String)     : AuthEffect
}
