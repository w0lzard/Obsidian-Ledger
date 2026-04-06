package com.ryuken.obsidianledger.di

import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import com.ryuken.obsidianledger.core.preferences.AppPreferences
import com.ryuken.obsidianledger.core.preferences.IosAppPreferences
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
    single<AppPreferences> { IosAppPreferences() }
}