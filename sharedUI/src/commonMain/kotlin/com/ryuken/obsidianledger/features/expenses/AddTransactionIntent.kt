package com.ryuken.obsidianledger.features.expenses

import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate

sealed interface AddTransactionIntent {
    data class NumpadInput(val key: String) : AddTransactionIntent
    data object NumpadDelete : AddTransactionIntent
    data class TypeChanged(val type: TransactionType) : AddTransactionIntent
    data class CategorySelected(val category: Category) : AddTransactionIntent
    data class NoteChanged(val note: String) : AddTransactionIntent
    data class DateChanged(val date: LocalDate) : AddTransactionIntent
    data object SaveClick : AddTransactionIntent
}
