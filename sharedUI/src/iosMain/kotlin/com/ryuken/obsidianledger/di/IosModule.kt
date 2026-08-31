package com.ryuken.obsidianledger.di

import com.russhwolf.settings.Settings
import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import com.ryuken.obsidianledger.core.preferences.AppPreferences
import com.ryuken.obsidianledger.core.preferences.IosAppPreferences
import com.ryuken.obsidianledger.core.preferences.KeychainSettings
import com.ryuken.obsidianledger.core.sync.IosSyncCoordinator
import com.ryuken.obsidianledger.core.sync.SyncCoordinator
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
    single<AppPreferences> { IosAppPreferences() }
    // Keychain-backed so Supabase session tokens are encrypted at rest, matching
    // Android's EncryptedSharedPreferences.
    single<Settings> { KeychainSettings() }
    single<SyncCoordinator> { IosSyncCoordinator() }
}
