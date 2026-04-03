package com.ryuken.obsidianledger.core.di

import com.ryuken.obsidianledger.BuildConfig
import com.ryuken.obsidianledger.core.database.LedgerDatabase
import com.ryuken.obsidianledger.core.database.createLedgerDatabase
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.repository.ProfileRepository
import com.ryuken.obsidianledger.core.data.TransactionRepositoryImpl
import com.ryuken.obsidianledger.core.data.BudgetRepositoryImpl
import com.ryuken.obsidianledger.core.data.CategoryRepositoryImpl
import com.ryuken.obsidianledger.core.data.AuthRepositoryImpl
import com.ryuken.obsidianledger.core.data.ProfileRepositoryImpl
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import org.koin.dsl.module
import com.ryuken.obsidianledger.core.network.SupabaseConfig


val coreModule = module {

    // ── Database ──────────────────────────────────────────────────────
    single { createLedgerDatabase(get()) }
    single { get<LedgerDatabase>().transactionEntityQueries }
    single { get<LedgerDatabase>().budgetEntityQueries }
    single { get<LedgerDatabase>().categoryEntityQueries }

    // ── Supabase client ───────────────────────────────────────────────
    single {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.url,
            supabaseKey = SupabaseConfig.key
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    // ── Repositories ──────────────────────────────────────────────────
    single<TransactionRepository> {
        TransactionRepositoryImpl(
            db             = get(),
            supabaseClient = get()
        )
    }
    single<BudgetRepository> {
        BudgetRepositoryImpl(
            db             = get(),
            supabaseClient = get()
        )
    }
    single<CategoryRepository> {
        CategoryRepositoryImpl(db = get())
    }
    single<AuthRepository> {
        AuthRepositoryImpl(supabaseClient = get())
    }
    single<ProfileRepository> {
        ProfileRepositoryImpl(supabaseClient = get())
    }
}