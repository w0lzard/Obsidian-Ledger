package com.ryuken.obsidianledger.core.preferences

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

class AndroidAppPreferences(context: Context) : AppPreferences {
    private val settings = SharedPreferencesSettings(
        context.getSharedPreferences("obsidian_ledger_prefs", Context.MODE_PRIVATE)
    )

    override fun getString(key: String, default: String): String = settings.getString(key, default)
    override fun putString(key: String, value: String) = settings.putString(key, value)
    override fun getBoolean(key: String, default: Boolean): Boolean = settings.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = settings.putBoolean(key, value)
}
