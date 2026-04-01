package com.ryuken.obsidianledger.core.domain.mapper

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.dto.BudgetDto

fun Budget.toDto() = BudgetDto(
    id           = id,
    category_id  = category.id,
    limit_amount = limitAmount,
    period       = period.name,
    user_id      = userId
)
