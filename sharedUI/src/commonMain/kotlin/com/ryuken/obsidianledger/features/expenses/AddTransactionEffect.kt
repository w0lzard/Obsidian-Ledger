package com.ryuken.obsidianledger.features.expenses

sealed interface AddTransactionEffect {
    data object SaveSuccess : AddTransactionEffect
    data class Error(val message: String) : AddTransactionEffect
}
