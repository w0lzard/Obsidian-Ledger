package com.ryuken.obsidianledger.core.di

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
    viewModel {
        DashboardViewModel(
            getMonthlySummary    = get(),
            getRecentTransactions = get(),
            getBudgetPreview     = get()
        )
    }

    // ── Expenses (Add Transaction) ────────────────────────────────────
    factory { AddTransactionUseCase(transactionRepo = get()) }
    factory { GetCategoriesUseCase(categoryRepo = get()) }
    viewModel {
        AddTransactionViewModel(
            addTransaction = get(),
            getCategories  = get(),
            userId         = get<AuthRepository>().currentUserId() ?: ""
        )
    }

    // ── Analytics ─────────────────────────────────────────────────────
    factory { GetMonthlyTotalsUseCase(transactionRepo = get()) }
    viewModel {
        AnalyticsViewModel(
            getMonthlySummary = get(),
            getMonthlyTotals  = get()
        )
    }

    // ── Budgets ───────────────────────────────────────────────────────
    factory { GetBudgetsWithSpendingUseCase(budgetRepo = get()) }
    factory { AddBudgetUseCase(budgetRepo = get()) }
    factory { DeleteBudgetUseCase(budgetRepo = get()) }
    viewModel {
        BudgetsViewModel(
            getBudgets  = get(),
            addBudget   = get(),
            deleteBudget = get()
        )
    }

    // ── Profile ───────────────────────────────────────────────────────
    factory { GetProfileUseCase(authRepo = get()) }
    factory { SignOutUseCase(authRepo = get()) }
    factory { ExportCsvUseCase(transactionRepo = get()) }
    viewModel {
        ProfileViewModel(
            getProfile  = get(),
            signOut     = get(),
            exportCsv   = get(),
            syncUseCase = get()
        )
    }
}