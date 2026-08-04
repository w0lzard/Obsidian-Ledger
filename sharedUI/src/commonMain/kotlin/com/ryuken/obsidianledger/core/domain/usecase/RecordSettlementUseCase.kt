package com.ryuken.obsidianledger.core.domain.usecase

import com.benasher44.uuid.uuid4
import com.ryuken.obsidianledger.core.domain.model.Settlement
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import com.ryuken.obsidianledger.features.expenses.AddTransactionUseCase
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

class RecordSettlementUseCase(
    private val repo: SplitRepository,
    private val addTransaction: AddTransactionUseCase,
    private val categoryRepo: CategoryRepository
) {
    // Books a personal transaction for the current user when they're one side of the
    // settlement: an expense if they paid it off, income if they received the payment.
    suspend operator fun invoke(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amount: Double,
        date: LocalDate,
        currentUserId: String,
        currentUserIsPayer: Boolean,
        currentUserIsReceiver: Boolean
    ): Settlement {
        val settlement = repo.recordSettlement(groupId, fromMemberId, toMemberId, amount, date)

        val transactionType = when {
            currentUserIsPayer    -> TransactionType.EXPENSE
            currentUserIsReceiver -> TransactionType.INCOME
            else                  -> null
        }
        if (transactionType != null) {
            val now = Clock.System.now()
            addTransaction(
                Transaction(
                    id        = uuid4().toString(),
                    amount    = amount,
                    type      = transactionType,
                    category  = categoryRepo.getDefaultCategory("cat_split"),
                    note      = "Settlement",
                    date      = date,
                    createdAt = now,
                    updatedAt = now,
                    isDirty   = true,
                    userId    = currentUserId
                )
            )
        }

        return settlement
    }
}
