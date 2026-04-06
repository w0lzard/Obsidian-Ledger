package com.ryuken.obsidianledger.features.profile

sealed interface ProfileIntent {
    data object Refresh              : ProfileIntent
    data object ExportCsv            : ProfileIntent
    data object SyncNow              : ProfileIntent
    data object SignOut               : ProfileIntent
    
    // Dialog Toggles
    data class ToggleCurrencyDialog(val open: Boolean)      : ProfileIntent
    data class ToggleThemeDialog(val open: Boolean)         : ProfileIntent
    data class ToggleEditProfileDialog(val open: Boolean)   : ProfileIntent
    data class ToggleChangePasswordDialog(val open: Boolean): ProfileIntent
    data class ToggleImportDialog(val open: Boolean)        : ProfileIntent

    // Actions
    data class SetCurrency(val currency: String)            : ProfileIntent
    data class SetTheme(val theme: String)                  : ProfileIntent
    data class ToggleNotifications(val enabled: Boolean)    : ProfileIntent
    data class UpdateDisplayName(val name: String)          : ProfileIntent
    data class UpdatePassword(val pass: String)             : ProfileIntent
}

sealed interface ProfileEffect {
    data class  CsvExported(val data: String) : ProfileEffect
    data object SignedOut                      : ProfileEffect
    data class  Error(val message: String)    : ProfileEffect
    data object SyncComplete                   : ProfileEffect
    data class  ShowMessage(val message: String) : ProfileEffect
}
