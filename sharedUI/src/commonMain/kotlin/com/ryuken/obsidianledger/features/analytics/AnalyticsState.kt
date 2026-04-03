package com.ryuken.obsidianledger.features.analytics

import com.ryuken.obsidianledger.core.domain.model.MonthlySummary

data class AnalyticsState(
    val selectedMonth       : Int = 0,
    val selectedYear        : Int = 0,
    val totalOutflow        : Double = 0.0,
    val previousOutflow     : Double = 0.0,
    val sparklineData       : List<Double> = emptyList(),
    val categoryBreakdown   : Map<String, Double> = emptyMap(),
    val totalIncome         : Double = 0.0,
    val isLoading           : Boolean = true,
    val error               : String? = null
) {
    val monthOverMonthDelta: Double
        get() = if (previousOutflow > 0) ((totalOutflow - previousOutflow) / previousOutflow) * 100 else 0.0

    val savingsRate: Double
        get() = if (totalIncome > 0) ((totalIncome - totalOutflow) / totalIncome) * 100 else 0.0

    val avgTransaction: Double
        get() = if (categoryBreakdown.isNotEmpty()) totalOutflow / categoryBreakdown.size else 0.0
}
