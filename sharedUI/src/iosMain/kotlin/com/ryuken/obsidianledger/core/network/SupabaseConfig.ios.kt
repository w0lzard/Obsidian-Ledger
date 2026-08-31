package com.ryuken.obsidianledger.core.network

import platform.Foundation.NSBundle

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object SupabaseConfig {
    // Lazily read from the app's Info.plist (keys SUPABASE_URL / SUPABASE_KEY, values
    // injected at build time from an xcconfig or the Xcode target's Info tab). An
    // explicit configure() call still wins — used by tests or debug overrides.
    private var _url: String? = null
    private var _key: String? = null

    actual val url: String get() = _url ?: readPlist("SUPABASE_URL").also { _url = it }
    actual val key: String get() = _key ?: readPlist("SUPABASE_KEY").also { _key = it }

    fun configure(url: String, key: String) {
        _url = url
        _key = key
    }

    private fun readPlist(key: String): String =
        NSBundle.mainBundle.objectForInfoDictionaryKey(key)?.toString() ?: ""
}
