package com.ryuken.obsidianledger.features.profile

sealed interface ProfileIntent {
    data object Refresh              : ProfileIntent
    data object ExportCsv            : ProfileIntent
    data object SyncNow              : ProfileIntent
    data object SignOut               : ProfileIntent
}

sealed interface ProfileEffect {
    data class  CsvExported(val data: String) : ProfileEffect
    data object SignedOut                      : ProfileEffect
    data class  Error(val message: String)    : ProfileEffect
    data object SyncComplete                   : ProfileEffect
}
