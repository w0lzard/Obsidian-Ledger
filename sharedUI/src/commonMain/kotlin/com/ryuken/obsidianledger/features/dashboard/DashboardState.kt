package com.ryuken.obsidianledger.features.dashboard

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.core.domain.model.Transaction

data class DashboardState(
    val userName           : String            = "...",
    val summary            : MonthlySummary    = MonthlySummary(0.0, 0.0, emptyMap()),
    val monthlyBudget      : Double            = 0.0,
    val recentTransactions : List<Transaction>  = emptyList(),
    val budgets            : List<Budget>       = emptyList(),
    val isLoading          : Boolean           = true,
    val error              : String?           = null
) {
    val balance: Double get() = summary.totalIncome - summary.totalExpense
    val netSavings: Double get() = balance
}
