package com.ryuken.obsidianledger.core.di

import com.ryuken.obsidianledger.core.database.DatabaseDriverFactory
import com.ryuken.obsidianledger.core.domain.repository.*
import com.ryuken.obsidianledger.database.LedgerDatabase
import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.auth.Auth
import io.github.jan_tennert.supabase.postgrest.Postgrest
import io.github.jan_tennert.supabase.realtime.Realtime
import io.github.jan_tennert.supabase.createSupabaseClient
import org.koin.dsl.module

val coreModule = module {

    // ── Database ──────────────────────────────────────────────────────
    single { 
        val driver = get<DatabaseDriverFactory>().createDriver()
        LedgerDatabase(driver)
    }
    single { get<LedgerDatabase>().transactionEntityQueries }
    single { get<LedgerDatabase>().budgetEntityQueries }
    single { get<LedgerDatabase>().categoryEntityQueries }

    // ── Supabase client ───────────────────────────────────────────────
    // Note: These should ideally be in a BuildConfig or similar, 
    // but for now we define them or use placeholders.
    single {
        createSupabaseClient(
            supabaseUrl = "YOUR_SUPABASE_URL",
            supabaseKey = "YOUR_SUPABASE_ANON_KEY"
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    // ── Repositories ──────────────────────────────────────────────────
    // Assuming implementations exist or will be created
    /*
    single<TransactionRepository> {
        TransactionRepositoryImpl(
            queries        = get(),
            supabaseClient = get()
        )
    }
    */
}
