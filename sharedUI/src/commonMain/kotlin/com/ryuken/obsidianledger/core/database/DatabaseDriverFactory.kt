package com.ryuken.obsidianledger.core.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createLedgerDatabase(factory: DatabaseDriverFactory): LedgerDatabase =
    LedgerDatabase(factory.createDriver())