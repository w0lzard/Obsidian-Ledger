package com.ryuken.obsidianledger.di

import com.russhwolf.settings.Settings
import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import com.ryuken.obsidianledger.core.preferences.AppPreferences
import com.ryuken.obsidianledger.core.preferences.IosAppPreferences
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
    single<AppPreferences> { IosAppPreferences() }
    // ponytail: NSUserDefaults-backed, not Keychain — multiplatform-settings has no
    // Keychain-backed Settings out of the box. Session tokens aren't encrypted-at-rest
    // on iOS yet. Upgrade path: a custom Settings actual wrapping Security.framework Keychain.
    single<Settings> { Settings() }
}
