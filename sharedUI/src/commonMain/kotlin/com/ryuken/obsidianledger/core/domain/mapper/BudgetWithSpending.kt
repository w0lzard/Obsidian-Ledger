package com.ryuken.obsidianledger.core.domain.mapper

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetPeriod
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.database.BudgetWithSpending

fun BudgetWithSpending.toDomain(category: Category) : Budget =
    Budget (
        id          = id,
        category    = category,
        limitAmount = limitAmount,
        spent       = spent,
        period      = BudgetPeriod.valueOf(period),
        userId      = userId,
        isDirty     = isDirty == 1L
    )
