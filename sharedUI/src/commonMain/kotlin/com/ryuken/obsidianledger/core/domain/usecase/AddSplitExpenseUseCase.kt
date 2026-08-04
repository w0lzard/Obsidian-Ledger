package com.ryuken.obsidianledger.core.domain.usecase

import com.benasher44.uuid.uuid4
import com.ryuken.obsidianledger.core.domain.model.SplitExpense
import com.ryuken.obsidianledger.core.domain.model.SplitShare
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import com.ryuken.obsidianledger.features.expenses.AddTransactionUseCase
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

class AddSplitExpenseUseCase(
    private val repo: SplitRepository,
    private val addTransaction: AddTransactionUseCase,
    private val categoryRepo: CategoryRepository
) {
    // Also books a personal transaction for the current user when they're the one who paid,
    // since they fronted the money out of their own account.
    suspend operator fun invoke(
        groupId: String,
        description: String,
        amount: Double,
        paidByMemberId: String,
        date: LocalDate,
        shares: List<SplitShare>,
        currentUserId: String,
        payerIsCurrentUser: Boolean
    ): SplitExpense {
        val expense = repo.addExpense(groupId, description, amount, paidByMemberId, date, shares)

        if (payerIsCurrentUser) {
            val now = Clock.System.now()
            addTransaction(
                Transaction(
                    id        = uuid4().toString(),
                    amount    = amount,
                    type      = TransactionType.EXPENSE,
                    category  = categoryRepo.getDefaultCategory("cat_split"),
                    note      = description,
                    date      = date,
                    createdAt = now,
                    updatedAt = now,
                    isDirty   = true,
                    userId    = currentUserId
                )
            )
        }

        return expense
    }
}
