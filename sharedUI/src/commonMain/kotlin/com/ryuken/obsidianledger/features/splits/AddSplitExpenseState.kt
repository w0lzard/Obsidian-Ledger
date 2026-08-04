package com.ryuken.obsidianledger.features.splits

import com.ryuken.obsidianledger.core.domain.model.SplitGroup

data class AddSplitExpenseState(
    val group: SplitGroup? = null,
    val description: String = "",
    val amount: String = "",
    val payerMemberId: String? = null,
    val participantMemberIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
) {
    val amountDouble: Double get() = amount.toDoubleOrNull() ?: 0.0
    val canSave: Boolean get() = amountDouble > 0 && description.isNotBlank() &&
        payerMemberId != null && participantMemberIds.isNotEmpty() && !isSaving
}
