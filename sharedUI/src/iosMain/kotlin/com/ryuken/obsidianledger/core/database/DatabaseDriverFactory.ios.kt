package com.ryuken.obsidianledger.core.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import com.ryuken.obsidianledger.core.database.LedgerDatabase
import io.github.aakira.napier.Napier

actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            schema   = LedgerDatabase.Schema,
            name     = "ledger.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(
                        foreignKeyConstraints = true
                    )
                )
            }
        )
}