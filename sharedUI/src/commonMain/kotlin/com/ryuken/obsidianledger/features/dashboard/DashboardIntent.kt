package com.ryuken.obsidianledger.features.dashboard

sealed interface DashboardIntent {
    data object Refresh                                          : DashboardIntent
    data class  MonthChanged(val month: Int, val year: Int)    : DashboardIntent
}