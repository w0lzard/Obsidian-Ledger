package com.ryuken.obsidianledger.features.budgets

import com.ryuken.obsidianledger.core.domain.model.Category

sealed interface BudgetsIntent {
    data object Refresh                                                    : BudgetsIntent
    data object AddBudgetClick                                             : BudgetsIntent
    data object DismissDialog                                              : BudgetsIntent
    data class  ConfirmAddBudget(val category: Category, val limit: Double): BudgetsIntent
    data class  DeleteBudget(val id: String)                               : BudgetsIntent
}

sealed interface BudgetsEffect {
    data object BudgetAdded                    : BudgetsEffect
    data object BudgetDeleted                  : BudgetsEffect
    data class  Error(val message: String)     : BudgetsEffect
}
