package com.ryuken.obsidianledger.core.network

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object SupabaseConfig {
    private var _url: String = ""
    private var _key: String = ""
    actual val url: String get() = _url
    actual val key: String get() = _key
    fun configure(url: String, key: String) {
        _url = url
        _key = key
    }
}
