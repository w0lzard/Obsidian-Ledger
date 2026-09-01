package com.ryuken.obsidianledger.features.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsStateTest {

    @Test
    fun avgTransaction_dividesByTransactionCount() {
        // 3 transactions totaling 300 across 2 categories -> avg 100, not 150.
        val state = AnalyticsState(
            totalOutflow      = 300.0,
            transactionCount  = 3,
            categoryBreakdown = mapOf("food" to 200.0, "bills" to 100.0)
        )
        assertEquals(100.0, state.avgTransaction)
    }

    @Test
    fun avgTransaction_zeroTransactions_isZero() {
        val state = AnalyticsState(totalOutflow = 0.0, transactionCount = 0)
        assertEquals(0.0, state.avgTransaction)
    }

    @Test
    fun savingsRate_andMonthOverMonth() {
        val state = AnalyticsState(
            totalOutflow    = 250.0,
            previousOutflow = 200.0,
            totalIncome     = 1000.0
        )
        assertEquals(25.0, state.monthOverMonthDelta)
        assertEquals(75.0, state.savingsRate)
    }
}
