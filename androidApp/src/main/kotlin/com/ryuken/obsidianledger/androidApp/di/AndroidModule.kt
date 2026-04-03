package com.ryuken.obsidianledger.androidApp.di

import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { SyncScheduler(androidContext()) }
}