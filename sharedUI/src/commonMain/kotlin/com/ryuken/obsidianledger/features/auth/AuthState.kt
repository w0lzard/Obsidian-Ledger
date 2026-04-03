package com.ryuken.obsidianledger.features.auth

data class AuthState(
    val activeTab    : AuthTab = AuthTab.SIGN_IN,
    val email        : String  = "",
    val password     : String  = "",
    val displayName  : String  = "",
    val isLoading    : Boolean = false,
    val error        : String? = null
)

enum class AuthTab { SIGN_IN, CREATE_ACCOUNT }
