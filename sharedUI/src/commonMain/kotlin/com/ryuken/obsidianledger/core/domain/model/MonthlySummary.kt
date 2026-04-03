package com.ryuken.obsidianledger.core.domain.model

data class MonthlySummary(
    val totalExpense      : Double,
    val totalIncome       : Double,
    val categoryBreakdown : Map<String, Double>
)
