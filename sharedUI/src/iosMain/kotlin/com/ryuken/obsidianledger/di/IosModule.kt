package com.ryuken.obsidianledger.di

import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
}