package com.ryuken.obsidianledger.features.budgets

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.Category

data class BudgetsState(
    val budgets       : List<Budget>    = emptyList(),
    val categories    : List<Category>  = emptyList(),
    val isLoading     : Boolean         = true,
    val error         : String?         = null,
    val showAddDialog : Boolean         = false
)
