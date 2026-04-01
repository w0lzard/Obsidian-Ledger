package com.ryuken.obsidianledger.core.domain.model

enum class BudgetStatus {
    HEALTHY,
    WARNING,
    HIGH_ALERT,
    CRITICAL,
    EXCEEDED;

    val isAlert : Boolean
        get() = this == HIGH_ALERT || this == CRITICAL || this == EXCEEDED
}