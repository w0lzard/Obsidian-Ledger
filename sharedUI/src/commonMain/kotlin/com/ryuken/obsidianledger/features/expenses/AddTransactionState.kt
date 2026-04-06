package com.ryuken.obsidianledger.features.expenses

import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class AddTransactionState(
    val amount : String = "",
    val type : TransactionType = TransactionType.EXPENSE,
    val selectedCategory : Category? = null,
    val note : String = "",
    val date : LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val isLoading : Boolean = false,
    val error : String? = null,
    val categories: List<Category> = emptyList()
){
    val amountDouble : Double get() = amount.toDoubleOrNull() ?: 0.0
    val canSave : Boolean get() = amountDouble > 0 && selectedCategory != null
}
