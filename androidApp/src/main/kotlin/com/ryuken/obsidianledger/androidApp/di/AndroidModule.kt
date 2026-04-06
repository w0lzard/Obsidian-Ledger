package com.ryuken.obsidianledger.androidApp.di

import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import com.ryuken.obsidianledger.core.preferences.AndroidAppPreferences
import com.ryuken.obsidianledger.core.preferences.AppPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { SyncScheduler(androidContext()) }
    single<AppPreferences> { AndroidAppPreferences(androidContext()) }
}