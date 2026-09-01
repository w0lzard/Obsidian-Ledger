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

    // Reactive so a ViewModel created before session restore finishes doesn't freeze
    // on a null/empty userId for its whole lifetime.
    private val userId: StateFlow<String?> = authRepo.observeUserId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepo.currentUserId())

    private val _year        = MutableStateFlow(today.year)
    private val _month       = MutableStateFlow(today.month.ordinal + 1)
    private val _refreshTick = MutableStateFlow(0)

    val state: StateFlow<AnalyticsState> = userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(AnalyticsState(
                selectedMonth = _month.value,
                selectedYear  = _year.value,
                isLoading     = true
            ))
        } else {
            combine(
                _year.flatMapLatest { y -> _month.flatMapLatest { m -> _refreshTick.flatMapLatest {
                    getMonthlySummary(uid, y, m)
                }}},
                _year.flatMapLatest { y -> _month.flatMapLatest { m -> _refreshTick.flatMapLatest {
                    getMonthlyTotals(uid, y, m, months = 6)
                }}}
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
                    transactionCount  = summary.transactionCount,
                    isLoading         = false
                )
            }
        }
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsState(
            selectedMonth = today.month.ordinal + 1,
            selectedYear  = today.year,
            isLoading     = true
        )
    )

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            AnalyticsIntent.Refresh         -> _refreshTick.update { it + 1 }
            is AnalyticsIntent.MonthChanged -> {
                _year.update  { intent.year  }
                _month.update { intent.month }
            }
        }
    }
}
