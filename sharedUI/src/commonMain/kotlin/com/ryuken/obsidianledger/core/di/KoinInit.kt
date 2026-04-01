package com.ryuken.obsidianledger.core.di

import org.koin.core.module.Module
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(
    platformModule: Module = module {}
) {
    startKoin {
        modules(
            platformModule,  // Android Context, iOS NSUserDefaults etc.
            coreModule,
            featureModule
        )
    }
}