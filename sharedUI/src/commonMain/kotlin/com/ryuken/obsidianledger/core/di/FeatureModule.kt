package com.ryuken.obsidianledger.core.di

import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.usecase.SyncUseCase
import com.ryuken.obsidianledger.features.auth.AuthViewModel
import com.ryuken.obsidianledger.features.auth.SignInUseCase
import com.ryuken.obsidianledger.features.auth.SignUpUseCase
import com.ryuken.obsidianledger.features.dashboard.DashboardViewModel
import com.ryuken.obsidianledger.features.dashboard.GetMonthlySummaryUseCase
import com.ryuken.obsidianledger.features.dashboard.GetRecentTransactionsUseCase
import com.ryuken.obsidianledger.features.dashboard.GetBudgetPreviewUseCase
import com.ryuken.obsidianledger.features.dashboard.GetProfileUseCase
import com.ryuken.obsidianledger.features.expenses.AddTransactionUseCase
import com.ryuken.obsidianledger.features.expenses.AddTransactionViewModel
import com.ryuken.obsidianledger.features.expenses.GetCategoriesUseCase
import com.ryuken.obsidianledger.features.analytics.AnalyticsViewModel
import com.ryuken.obsidianledger.features.analytics.GetMonthlyTotalsUseCase
import com.ryuken.obsidianledger.features.budgets.BudgetsViewModel
import com.ryuken.obsidianledger.features.budgets.GetBudgetsWithSpendingUseCase
import com.ryuken.obsidianledger.features.budgets.AddBudgetUseCase
import com.ryuken.obsidianledger.features.budgets.DeleteBudgetUseCase
import com.ryuken.obsidianledger.features.profile.ProfileViewModel
import com.ryuken.obsidianledger.features.profile.SignOutUseCase
import com.ryuken.obsidianledger.features.profile.ExportCsvUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureModule = module {

    // ── Auth ──────────────────────────────────────────────────────────
    factory { SignInUseCase(authRepo = get()) }
    factory { SignUpUseCase(authRepo = get()) }
    viewModel {
        AuthViewModel(
            signIn = get(),
            signUp = get()
        )
    }

    // ── Dashboard ─────────────────────────────────────────────────────
    factory { GetMonthlySummaryUseCase(transactionRepo = get()) }
    factory { GetRecentTransactionsUseCase(transactionRepo = get()) }
    factory { GetBudgetPreviewUseCase(budgetRepo = get()) }
    factory { GetProfileUseCase(profileRepo = get()) }
    viewModel {
        DashboardViewModel(
            getMonthlySummary     = get(),
            getRecentTransactions = get(),
            getBudgetPreview      = get(),
            getProfile            = get(),
            authRepo              = get()
        )
    }

    // ── Expenses (Add Transaction) ────────────────────────────────────
    factory { AddTransactionUseCase(repository = get()) }
    factory { GetCategoriesUseCase(repository = get()) }
    viewModel {
        AddTransactionViewModel(
            addTransaction = get(),
            getCategories  = get(),
            authRepo       = get()
        )
    }

    // ── Analytics ─────────────────────────────────────────────────────
    factory { GetMonthlyTotalsUseCase(transactionRepo = get()) }
    viewModel {
        AnalyticsViewModel(
            getMonthlySummary = get(),
            getMonthlyTotals  = get(),
            authRepo          = get()
        )
    }

    // ── Budgets ───────────────────────────────────────────────────────
    factory { GetBudgetsWithSpendingUseCase(budgetRepo = get()) }
    factory { AddBudgetUseCase(budgetRepo = get()) }
    factory { DeleteBudgetUseCase(budgetRepo = get()) }
    viewModel {
        BudgetsViewModel(
            getBudgets   = get(),
            addBudget    = get(),
            deleteBudget = get(),
            getCategories = get(),
            authRepo     = get()
        )
    }

    // ── Profile ───────────────────────────────────────────────────────
    factory { SignOutUseCase(authRepo = get()) }
    factory { ExportCsvUseCase(transactionRepo = get()) }
    factory { SyncUseCase(transactionRepo = get(), budgetRepo = get()) }
    viewModel {
        ProfileViewModel(
            getProfile  = get(),
            signOut     = get(),
            exportCsv   = get(),
            syncUseCase = get(),
            authRepo    = get()
        )
    }
}