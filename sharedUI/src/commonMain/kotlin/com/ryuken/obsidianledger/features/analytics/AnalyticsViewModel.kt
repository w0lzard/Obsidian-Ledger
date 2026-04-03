package com.ryuken.obsidianledger.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.features.dashboard.GetMonthlySummaryUseCase
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class AnalyticsViewModel(
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val getMonthlyTotals: GetMonthlyTotalsUseCase,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val today  = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val userId = authRepo.currentUserId() ?: ""

    private val _year  = MutableStateFlow(today.year)
    private val _month = MutableStateFlow(today.monthNumber)

    val state: StateFlow<AnalyticsState> = combine(
        _year.flatMapLatest { y -> _month.flatMapLatest { m ->
            getMonthlySummary(userId, y, m)
        }},
        _year.flatMapLatest { y -> _month.flatMapLatest { m ->
            getMonthlyTotals(userId, y, m, months = 6)
        }}
    ) { summary, monthlyTotals ->
        val sparkline = monthlyTotals.map { it.third.totalExpense }
        val previousExpense = if (monthlyTotals.size >= 2)
            monthlyTotals[monthlyTotals.size - 2].third.totalExpense else 0.0

        AnalyticsState(
            selectedMonth     = _month.value,
            selectedYear      = _year.value,
            totalOutflow      = summary.totalExpense,
            previousOutflow   = previousExpense,
            sparklineData     = sparkline,
            categoryBreakdown = summary.categoryBreakdown,
            totalIncome       = summary.totalIncome,
            isLoading         = false
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsState(
            selectedMonth = today.monthNumber,
            selectedYear  = today.year,
            isLoading     = true
        )
    )

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            AnalyticsIntent.Refresh         -> _month.update { it }
            is AnalyticsIntent.MonthChanged -> {
                _year.update  { intent.year  }
                _month.update { intent.month }
            }
        }
    }
}
