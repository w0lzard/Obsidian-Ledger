package com.ryuken.obsidianledger.features.analytics

sealed interface AnalyticsIntent {
    data class MonthChanged(val month: Int, val year: Int) : AnalyticsIntent
    data object Refresh : AnalyticsIntent
}

sealed interface AnalyticsEffect {
    data class Error(val message: String) : AnalyticsEffect
}
