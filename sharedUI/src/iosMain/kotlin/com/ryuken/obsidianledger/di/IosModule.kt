package com.ryuken.obsidianledger.di

import com.russhwolf.settings.Settings
import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import com.ryuken.obsidianledger.core.preferences.AppPreferences
import com.ryuken.obsidianledger.core.preferences.IosAppPreferences
import com.ryuken.obsidianledger.core.sync.SyncCoordinator
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
    single<AppPreferences> { IosAppPreferences() }
    // ponytail: NSUserDefaults-backed, not Keychain — multiplatform-settings has no
    // Keychain-backed Settings out of the box. Session tokens aren't encrypted-at-rest
    // on iOS yet. Upgrade path: a custom Settings actual wrapping Security.framework Keychain.
    single<Settings> { Settings() }
    // ponytail: no periodic background sync exists on iOS yet (Android-only WorkManager today).
    // No-op until an iOS equivalent (BGTaskScheduler) is built.
    single<SyncCoordinator> { object : SyncCoordinator {
        override fun onSignedOut() {}
    } }
}
