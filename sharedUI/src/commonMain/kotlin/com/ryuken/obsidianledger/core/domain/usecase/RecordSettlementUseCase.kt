package com.ryuken.obsidianledger.core.domain.usecase

import com.benasher44.uuid.uuid4
import com.ryuken.obsidianledger.core.domain.helper.roundToCents
import com.ryuken.obsidianledger.core.domain.model.Settlement
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import com.ryuken.obsidianledger.features.expenses.AddTransactionUseCase
import kotlinx.coroutines.flow.first
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
        // Business invariant: a settlement may not exceed what the payer still owes.
        // Pre-check here (net-balance based) surfaces a clear message in the UI; the
        // record_split_settlement RPC re-validates exactly pairwise server-side.
        require(amount.roundToCents() > 0.0) { "Settlement amount must be positive" }
        val payerNet = repo.observeBalances(groupId).first()
            .firstOrNull { it.memberId == fromMemberId }?.netAmount ?: 0.0
        val owedByPayer = (-payerNet).roundToCents()
        require(amount.roundToCents() <= owedByPayer + 0.005) {
            "Settlement of $amount exceeds the outstanding balance of $owedByPayer"
        }

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
