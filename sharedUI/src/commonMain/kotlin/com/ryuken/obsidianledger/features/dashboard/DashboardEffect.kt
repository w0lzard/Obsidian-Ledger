package com.ryuken.obsidianledger.features.dashboard

sealed interface DashboardEffect {
    data class Error(val message: String) : DashboardEffect
}
