package com.ryuken.obsidianledger.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class DashboardViewModel(
    private val getMonthlySummary     : GetMonthlySummaryUseCase,
    private val getRecentTransactions : GetRecentTransactionsUseCase,
    private val getBudgetPreview      : GetBudgetPreviewUseCase,
    private val getProfile            : GetProfileUseCase,
    private val authRepo              : AuthRepository
) : ViewModel() {

    private val today  = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val userId = authRepo.currentUserId() ?: ""

    private val _year     = MutableStateFlow(today.year)
    private val _month    = MutableStateFlow(today.monthNumber)
    private val _userName = MutableStateFlow("...")

    init {
        viewModelScope.launch {
            runCatching { getProfile(userId) }
                .onSuccess { _userName.update { _ -> it.displayName } }
                .onFailure { _userName.update { _ -> "User" } }
        }
    }

    val state: StateFlow<DashboardState> = combine(
        _userName,
        _year.flatMapLatest { y -> _month.flatMapLatest { m ->
            getMonthlySummary(userId, y, m)
        }},
        _year.flatMapLatest { y -> _month.flatMapLatest { m ->
            getRecentTransactions(userId, y, m, limit = 10)
        }},
        _year.flatMapLatest { y -> _month.flatMapLatest { m ->
            getBudgetPreview(userId, y, m, maxBudgets = 3)
        }}
    ) { name, summary, transactions, budgets ->
        DashboardState(
            userName           = name,
            summary            = summary,
            monthlyBudget      = budgets.sumOf { it.limitAmount },
            recentTransactions = transactions,
            budgets            = budgets,
            isLoading          = false
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState(isLoading = true)
    )

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Refresh           -> _month.update { it }
            is DashboardIntent.MonthChanged   -> {
                _year.update  { intent.year  }
                _month.update { intent.month }
            }
        }
    }
}

// ─── Dynamic greeting helper ──────────────────────────────────────────
fun greeting(): String {
    val hour = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }
}
