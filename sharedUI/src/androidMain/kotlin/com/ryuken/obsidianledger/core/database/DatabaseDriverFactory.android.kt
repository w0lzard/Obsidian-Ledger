package com.ryuken.obsidianledger.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ryuken.obsidianledger.core.database.LedgerDatabase
import io.github.aakira.napier.Napier

actual class DatabaseDriverFactory(private val context: Context) {

    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema   = LedgerDatabase.Schema,
            context  = context,
            name     = "ledger.db",
            callback = object : AndroidSqliteDriver.Callback(LedgerDatabase.Schema) {
                override fun onUpgrade(
                    db        : androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    Napier.d("DB upgrading from $oldVersion to $newVersion")
                    runMigrations(db, oldVersion, newVersion)
                }
            }
        )
    }

    private fun runMigrations(
        db         : androidx.sqlite.db.SupportSQLiteDatabase,
        oldVersion : Int,
        newVersion : Int
    ) {
        if (oldVersion < 2) {
            Napier.d("Running migration to version 2 — adding split tables")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS SplitGroupEntity (
                    id          TEXT    NOT NULL PRIMARY KEY,
                    name        TEXT    NOT NULL,
                    emoji       TEXT    NOT NULL DEFAULT '👥',
                    createdBy   TEXT    NOT NULL,
                    createdAt   TEXT    NOT NULL,
                    isDirty     INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS SplitMemberEntity (
                    id          TEXT    NOT NULL PRIMARY KEY,
                    groupId     TEXT    NOT NULL,
                    displayName TEXT    NOT NULL,
                    email       TEXT,
                    userId      TEXT,
                    isDirty     INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS SplitExpenseEntity (
                    id          TEXT    NOT NULL PRIMARY KEY,
                    groupId     TEXT    NOT NULL,
                    description TEXT    NOT NULL,
                    totalAmount REAL    NOT NULL,
                    paidBy      TEXT    NOT NULL,
                    date        TEXT    NOT NULL,
                    createdAt   TEXT    NOT NULL,
                    isDirty     INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS SplitShareEntity (
                    id          TEXT    NOT NULL PRIMARY KEY,
                    expenseId   TEXT    NOT NULL,
                    memberId    TEXT    NOT NULL,
                    memberName  TEXT    NOT NULL,
                    owedAmount  REAL    NOT NULL,
                    isPaid      INTEGER NOT NULL DEFAULT 0,
                    isDirty     INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS SettlementEntity (
                    id          TEXT    NOT NULL PRIMARY KEY,
                    groupId     TEXT    NOT NULL,
                    fromMember  TEXT    NOT NULL,
                    toMember    TEXT    NOT NULL,
                    amount      REAL    NOT NULL,
                    date        TEXT    NOT NULL,
                    isDirty     INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())
        }
    }
}
