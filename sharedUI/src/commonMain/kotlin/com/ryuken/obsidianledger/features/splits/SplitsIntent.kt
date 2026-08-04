package com.ryuken.obsidianledger.features.splits

sealed interface SplitsIntent {
    data object Refresh : SplitsIntent
    data class CreateGroup(val name: String, val memberNames: List<String>) : SplitsIntent
}

sealed interface SplitsEffect {
    data class GroupCreated(val groupId: String) : SplitsEffect
    data class Error(val message: String) : SplitsEffect
}
