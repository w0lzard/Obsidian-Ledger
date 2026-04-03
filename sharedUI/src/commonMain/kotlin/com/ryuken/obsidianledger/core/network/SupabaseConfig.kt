package com.ryuken.obsidianledger.core.network

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object SupabaseConfig {
    val url: String
    val key: String
}
