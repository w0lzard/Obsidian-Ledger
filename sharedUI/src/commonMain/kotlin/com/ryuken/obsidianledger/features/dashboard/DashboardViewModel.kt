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
    private val getGroups             : com.ryuken.obsidianledger.core.domain.usecase.GetGroupsUseCase,
    private val authRepo              : AuthRepository
) : ViewModel() {

    private val today  = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val userId = authRepo.currentUserId() ?: ""

    private val _year     = MutableStateFlow(today.year)
    private val _month    = MutableStateFlow(today.monthNumber)
    private val _userName = MutableStateFlow("...")

    init {
        viewModelScope.launch {
            // Prefer the profile display name; fall back to auth user metadata if profile fetch fails.
            runCatching { getProfile(userId) }
                .onSuccess { _userName.update { _ -> it.displayName.ifBlank { "You" } } }
                .onFailure {
                    val fallback = authRepo.currentUserId()?.let { _ -> "You" } ?: "You"
                    _userName.update { _ -> fallback }
                }
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
        }},
        getGroups(userId)
    ) { args ->
        val name = args[0] as String
        val summary = args[1] as com.ryuken.obsidianledger.core.domain.model.MonthlySummary
        @Suppress("UNCHECKED_CAST") val transactions = args[2] as List<com.ryuken.obsidianledger.core.domain.model.Transaction>
        @Suppress("UNCHECKED_CAST") val budgets = args[3] as List<com.ryuken.obsidianledger.core.domain.model.Budget>
        @Suppress("UNCHECKED_CAST") val groups = args[4] as List<com.ryuken.obsidianledger.core.domain.model.SplitGroup>
        
        DashboardState(
            userName           = name,
            summary            = summary,
            monthlyBudget      = budgets.sumOf { it.limitAmount },
            recentTransactions = transactions,
            budgets            = budgets,
            activeSplitGroups  = groups.size,
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
    return "Greetings"
}
