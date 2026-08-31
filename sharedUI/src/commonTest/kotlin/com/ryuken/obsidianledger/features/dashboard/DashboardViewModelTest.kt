package com.ryuken.obsidianledger.features.dashboard

import com.ryuken.obsidianledger.core.domain.helper.nextMonth
import com.ryuken.obsidianledger.core.domain.helper.previousMonth
import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetPeriod
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.core.domain.usecase.GetGroupsUseCase
import com.ryuken.obsidianledger.fake.FakeAuthRepository
import com.ryuken.obsidianledger.fake.FakeBudgetRepository
import com.ryuken.obsidianledger.fake.FakeProfileRepository
import com.ryuken.obsidianledger.fake.FakeSplitRepository
import com.ryuken.obsidianledger.fake.FakeTransactionRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var txRepo: FakeTransactionRepository
    private lateinit var budgetRepo: FakeBudgetRepository

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel(): DashboardViewModel = DashboardViewModel(
        getMonthlySummary     = GetMonthlySummaryUseCase(txRepo),
        getRecentTransactions = GetRecentTransactionsUseCase(txRepo),
        getBudgetPreview      = GetBudgetPreviewUseCase(budgetRepo),
        getProfile            = GetProfileUseCase(FakeProfileRepository()),
        getGroups             = GetGroupsUseCase(FakeSplitRepository()),
        authRepo              = FakeAuthRepository()
    )

    private fun DashboardViewModel.collectLastStates(): Pair<List<DashboardState>, kotlinx.coroutines.Job> {
        val states = mutableListOf<DashboardState>()
        val job = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined).launch { state.toList(states) }
        return states to job
    }

    @Test
    fun initialLoad_exposesCurrentMonthAndStopsLoading() = runBlocking {
        txRepo = FakeTransactionRepository()
        budgetRepo = FakeBudgetRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()

        val loaded = states.last()
        assertTrue(!loaded.isLoading)
        assertTrue(loaded.selectedMonth in 1..12)
        job.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun monthChangeForward_updatesSelectedMonth() = runBlocking {
        txRepo = FakeTransactionRepository()
        budgetRepo = FakeBudgetRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()
        val initial = states.last()

        val expected = nextMonth(initial.selectedYear, initial.selectedMonth)
        vm.onIntent(DashboardIntent.MonthChanged(month = expected.second, year = expected.first))

        assertEquals(expected.second, states.last().selectedMonth)
        assertEquals(expected.first, states.last().selectedYear)
        job.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun monthChangeBackward_updatesSelectedMonth() = runBlocking {
        txRepo = FakeTransactionRepository()
        budgetRepo = FakeBudgetRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()
        val initial = states.last()

        val expected = previousMonth(initial.selectedYear, initial.selectedMonth)
        vm.onIntent(DashboardIntent.MonthChanged(month = expected.second, year = expected.first))

        assertEquals(expected.second, states.last().selectedMonth)
        job.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun yearRollover_forwardAcrossDecember() = runBlocking {
        txRepo = FakeTransactionRepository()
        budgetRepo = FakeBudgetRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()

        vm.onIntent(DashboardIntent.MonthChanged(month = 12, year = 2026))
        vm.onIntent(DashboardIntent.MonthChanged(month = 1, year = 2027))

        assertEquals(2027, states.last().selectedYear)
        assertEquals(1, states.last().selectedMonth)
        job.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun refresh_recollectsFlows() = runBlocking {
        txRepo = FakeTransactionRepository()
        budgetRepo = FakeBudgetRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()
        assertEquals(0.0, states.last().summary.totalExpense)

        // New data lands in the source; a refresh tick pulls it through the pipeline.
        val (y, m) = states.last().selectedYear to states.last().selectedMonth
        txRepo.monthlySummaries.value = mapOf(
            y to m to MonthlySummary(totalExpense = 100.0, totalIncome = 200.0, categoryBreakdown = emptyMap())
        )
        vm.onIntent(DashboardIntent.Refresh)

        assertEquals(100.0, states.last().summary.totalExpense)
        job.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun rapidConsecutiveMonthChanges_onlyLatestSurvives() = runBlocking {
        txRepo = FakeTransactionRepository()
        budgetRepo = FakeBudgetRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()

        (1..20).forEach { vm.onIntent(DashboardIntent.MonthChanged(month = 6, year = 2026)) }
        vm.onIntent(DashboardIntent.MonthChanged(month = 8, year = 2026))

        assertEquals(8, states.last().selectedMonth)
        assertEquals(2026, states.last().selectedYear)
        job.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun monthlyBudget_sumsAllBudgets_beyondPreviewSlice() = runBlocking {
        txRepo = FakeTransactionRepository()
        budgetRepo = FakeBudgetRepository()
        val cat = Category(id = "c1", name = "C", emoji = "💰")
        val (y, m) = 2026 to 6
        budgetRepo.budgetsByMonth.value = mapOf(
            y to m to (1..5).map { i ->
                Budget(id = "b$i", category = cat, limitAmount = 10.0 * i, spent = 0.0, period = BudgetPeriod.MONTHLY, userId = "user-1")
            }
        )
        val vm = viewModel()
        vm.onIntent(DashboardIntent.MonthChanged(month = m, year = y))
        val (states, job) = vm.collectLastStates()

        assertEquals(150.0, states.last().monthlyBudget)   // 10+20+30+40+50
        assertEquals(3, states.last().budgets.size)        // preview strip stays capped
        job.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun monthlyBudget_zeroBudgets_isZero() = runBlocking {
        txRepo = FakeTransactionRepository()
        budgetRepo = FakeBudgetRepository()
        val vm = viewModel()
        val (states, job) = vm.collectLastStates()
        assertEquals(0.0, states.last().monthlyBudget)
        job.cancel()
        vm.viewModelScope.cancel()
    }
}
