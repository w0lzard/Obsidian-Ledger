package com.ryuken.obsidianledger.features.analytics

import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.helper.nextMonth
import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.features.dashboard.GetMonthlySummaryUseCase
import com.ryuken.obsidianledger.fake.FakeAuthRepository
import com.ryuken.obsidianledger.fake.FakeTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsViewModelTest {

    private lateinit var txRepo: FakeTransactionRepository

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel() = AnalyticsViewModel(
        getMonthlySummary = GetMonthlySummaryUseCase(txRepo),
        getMonthlyTotals  = GetMonthlyTotalsUseCase(txRepo),
        authRepo          = FakeAuthRepository()
    )

    private fun AnalyticsViewModel.collectLastStates(): Pair<List<AnalyticsState>, kotlinx.coroutines.Job> {
        val states = mutableListOf<AnalyticsState>()
        val job = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined).launch { state.toList(states) }
        return states to job
    }

    @Test
    fun initialLoad_stopsLoadingWithCurrentMonth() = runBlocking {
        txRepo = FakeTransactionRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()

        assertTrue(!states.last().isLoading)
        assertTrue(states.last().selectedMonth in 1..12)

        job.cancel(); vm.viewModelScope.cancel()
    }

    @Test
    fun monthChange_updatesSelection_andFetchesThatMonthsSummary() = runBlocking {
        txRepo = FakeTransactionRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()
        val initial = states.last()

        val target = nextMonth(initial.selectedYear, initial.selectedMonth)
        txRepo.monthlySummaries.value = mapOf(
            target to MonthlySummary(totalExpense = 77.0, totalIncome = 0.0, emptyMap(), transactionCount = 7)
        )
        vm.onIntent(AnalyticsIntent.MonthChanged(month = target.second, year = target.first))

        assertEquals(target.second, states.last().selectedMonth)
        assertEquals(77.0, states.last().totalOutflow)
        assertEquals(7, states.last().transactionCount)
        assertEquals(11.0, states.last().avgTransaction)   // 77 / 7 transactions

        job.cancel(); vm.viewModelScope.cancel()
    }

    @Test
    fun refresh_pullsNewDataThroughPipeline() = runBlocking {
        txRepo = FakeTransactionRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()
        val (y, m) = states.last().selectedYear to states.last().selectedMonth

        txRepo.monthlySummaries.value = mapOf(
            y to m to MonthlySummary(totalExpense = 55.0, totalIncome = 0.0, emptyMap(), transactionCount = 5)
        )
        vm.onIntent(AnalyticsIntent.Refresh)

        assertEquals(55.0, states.last().totalOutflow)

        job.cancel(); vm.viewModelScope.cancel()
    }

    @Test
    fun rapidMonthSwitches_lastWins() = runBlocking {
        txRepo = FakeTransactionRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()

        repeat(15) { vm.onIntent(AnalyticsIntent.MonthChanged(month = 3, year = 2026)) }
        vm.onIntent(AnalyticsIntent.MonthChanged(month = 11, year = 2026))

        assertEquals(11, states.last().selectedMonth)
        assertEquals(2026, states.last().selectedYear)

        job.cancel(); vm.viewModelScope.cancel()
    }
}
