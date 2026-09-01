package com.ryuken.obsidianledger.core.preferences

interface AppPreferences {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)

    companion object {
        const val KEY_THEME = "theme"
        const val KEY_CURRENCY = "currency"
        const val KEY_NOTIFICATIONS = "notifications_enabled"

        // ISO-8601 instant of the last successful sync; display text is derived
        // (relativeTime). Legacy installs may still hold the literal "Just now",
        // which the derive step treats as "Never".
        const val KEY_LAST_SYNC = "last_sync"
    }
}
