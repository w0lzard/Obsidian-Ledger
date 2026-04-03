package com.ryuken.obsidianledger.core.network

import com.ryuken.obsidianledger.BuildConfig

actual object SupabaseConfig {
    actual val url: String = BuildConfig.SUPABASE_URL
    actual val key: String = BuildConfig.SUPABASE_KEY
}
