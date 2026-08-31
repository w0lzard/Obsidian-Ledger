package com.ryuken.obsidianledger.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.helper.roundToCents
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

class DashboardViewModel(
    private val getMonthlySummary     : GetMonthlySummaryUseCase,
    private val getRecentTransactions : GetRecentTransactionsUseCase,
    private val getBudgetPreview      : GetBudgetPreviewUseCase,
    private val getProfile            : GetProfileUseCase,
    private val getGroups             : com.ryuken.obsidianledger.core.domain.usecase.GetGroupsUseCase,
    private val authRepo              : AuthRepository
) : ViewModel() {

    private val today  = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Reactive so a ViewModel created before session restore finishes doesn't freeze
    // on a null/empty userId for its whole lifetime.
    private val userId: StateFlow<String?> = authRepo.observeUserId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepo.currentUserId())

    private val _year        = MutableStateFlow(today.year)
    private val _month       = MutableStateFlow(today.month.ordinal + 1)
    private val _userName    = MutableStateFlow("...")
    private val _refreshTick = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            val uid = userId.filterNotNull().first()
            // Prefer the profile display name; fall back to auth user metadata if profile fetch fails.
            runCatching { getProfile(uid) }
                .onSuccess { _userName.update { _ -> it.displayName.ifBlank { "You" } } }
                .onFailure {
                    _userName.update { _ -> "You" }
                }
        }
    }

    val state: StateFlow<DashboardState> = userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(DashboardState(isLoading = true))
        } else {
            combine(
                _userName,
                _year,
                _month,
                _year.flatMapLatest { y -> _month.flatMapLatest { m -> _refreshTick.flatMapLatest {
                    getMonthlySummary(uid, y, m)
                }}},
                _year.flatMapLatest { y -> _month.flatMapLatest { m -> _refreshTick.flatMapLatest {
                    getRecentTransactions(uid, y, m, limit = 10)
                }}},
                _year.flatMapLatest { y -> _month.flatMapLatest { m -> _refreshTick.flatMapLatest {
                    getBudgetPreview(uid, y, m)
                }}},
                getGroups(uid)
            ) { args ->
                val name = args[0] as String
                val year = args[1] as Int
                val month = args[2] as Int
                val summary = args[3] as com.ryuken.obsidianledger.core.domain.model.MonthlySummary
                @Suppress("UNCHECKED_CAST") val transactions = args[4] as List<com.ryuken.obsidianledger.core.domain.model.Transaction>
                @Suppress("UNCHECKED_CAST") val budgets = args[5] as List<com.ryuken.obsidianledger.core.domain.model.Budget>
                @Suppress("UNCHECKED_CAST") val groups = args[6] as List<com.ryuken.obsidianledger.core.domain.model.SplitGroup>

                DashboardState(
                    userName           = name,
                    selectedYear       = year,
                    selectedMonth      = month,
                    summary            = summary,
                    // Total covers ALL budgets; the strip below shows the top 3.
                    monthlyBudget      = budgets.sumOf { it.limitAmount }.roundToCents(),
                    recentTransactions = transactions,
                    budgets            = budgets.take(PREVIEW_BUDGETS),
                    activeSplitGroups  = groups.size,
                    isLoading          = false
                )
            }
        }
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState(isLoading = true)
    )

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Refresh         -> _refreshTick.update { it + 1 }
            is DashboardIntent.MonthChanged -> {
                _year.update  { intent.year  }
                _month.update { intent.month }
            }
        }
    }

    private companion object {
        const val PREVIEW_BUDGETS = 3
    }
}

// ─── Dynamic greeting helper ──────────────────────────────────────────
// Pure and injectable so it is testable without a wall clock.
fun greeting(hourOfDay: Int): String = when (hourOfDay) {
    in 5..11  -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else      -> "Greetings"
}

fun greeting(now: Instant = Clock.System.now()): String =
    greeting(now.toLocalDateTime(TimeZone.currentSystemDefault()).hour)
