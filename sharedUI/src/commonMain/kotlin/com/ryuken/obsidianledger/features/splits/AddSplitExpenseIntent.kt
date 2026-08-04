package com.ryuken.obsidianledger.features.splits

sealed interface AddSplitExpenseIntent {
    data class DescriptionChanged(val value: String) : AddSplitExpenseIntent
    data class AmountChanged(val value: String) : AddSplitExpenseIntent
    data class PayerSelected(val memberId: String) : AddSplitExpenseIntent
    data class ToggleParticipant(val memberId: String) : AddSplitExpenseIntent
    data object SaveClick : AddSplitExpenseIntent
}

sealed interface AddSplitExpenseEffect {
    data object SaveSuccess : AddSplitExpenseEffect
    data class Error(val message: String) : AddSplitExpenseEffect
}
