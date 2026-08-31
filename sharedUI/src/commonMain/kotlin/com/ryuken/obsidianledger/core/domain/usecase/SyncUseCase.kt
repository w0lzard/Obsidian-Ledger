package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

/**
 * One full bidirectional sync run: push, then pull, per repository.
 *
 * Ordering and conflict policy are documented in docs/SyncContract.md. In short:
 * push-before-pull makes local dirty state win over remote changes, remote
 * deletions propagate to clean local rows, and server `updated_at` (maintained by
 * trigger) is the only change-detection token. Categories sync first so pulled
 * transactions and budgets always find their category rows locally.
 *
 * Each step is attempted even if an earlier one failed, so a budgets failure does
 * not block transaction sync; the first failure is rethrown at the end so callers
 * (WorkManager retry, manual SyncNow) still see the error.
 */
class SyncUseCase(
    private val transactionRepo: TransactionRepository,
    private val budgetRepo: BudgetRepository,
    private val categoryRepo: CategoryRepository
) {
    suspend operator fun invoke(userId: String) {
        var firstFailure: Exception? = null

        suspend fun attempt(step: String, block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("Sync step failed: $step", e)
                if (firstFailure == null) firstFailure = e
            }
        }

        attempt("categories.push") { categoryRepo.syncPendingToRemote(userId) }
        attempt("categories.pull") { categoryRepo.pullRemote(userId) }
        attempt("budgets.push")    { budgetRepo.syncPendingToRemote(userId) }
        attempt("budgets.pull")    { budgetRepo.pullRemote(userId) }
        attempt("transactions.push") { transactionRepo.syncPendingToRemote(userId) }
        attempt("transactions.pull") { transactionRepo.pullRemote(userId) }

        firstFailure?.let { throw it }
    }
}
