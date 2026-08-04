package com.ryuken.obsidianledger.core.preferences

import com.russhwolf.settings.Settings

class IosAppPreferences : AppPreferences {
    private val settings = Settings()

    override fun getString(key: String, default: String): String = settings.getString(key, default)
    override fun putString(key: String, value: String) = settings.putString(key, value)
    override fun getBoolean(key: String, default: Boolean): Boolean = settings.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = settings.putBoolean(key, value)
}
