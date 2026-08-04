package com.ryuken.obsidianledger.androidApp.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import com.ryuken.obsidianledger.core.preferences.AndroidAppPreferences
import com.ryuken.obsidianledger.core.preferences.AppPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { SyncScheduler(androidContext()) }
    single<AppPreferences> { AndroidAppPreferences(androidContext()) }
    // Session tokens (JWT/refresh token) need encryption-at-rest — plain SharedPreferences
    // is readable via adb backup or root. Backed by AndroidX Security's EncryptedSharedPreferences.
    single<Settings> { encryptedSessionSettings(androidContext()) }
}

private fun encryptedSessionSettings(context: Context): Settings {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "obsidian_ledger_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    return SharedPreferencesSettings(encryptedPrefs)
}
