package com.ryuken.obsidianledger.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            schema = LedgerDatabase.Schema,
            name = "ledger.db"
        )
}