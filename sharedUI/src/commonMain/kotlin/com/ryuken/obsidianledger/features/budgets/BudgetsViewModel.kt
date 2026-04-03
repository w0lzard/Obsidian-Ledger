package com.ryuken.obsidianledger.features.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetPeriod
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.features.expenses.GetCategoriesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class BudgetsViewModel(
    private val getBudgets    : GetBudgetsWithSpendingUseCase,
    private val addBudget     : AddBudgetUseCase,
    private val deleteBudget  : DeleteBudgetUseCase,
    private val getCategories : GetCategoriesUseCase,
    private val authRepo      : AuthRepository
) : ViewModel() {

    private val today  = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val userId = authRepo.currentUserId() ?: ""

    private val _showDialog = MutableStateFlow(false)

    private val _effect = Channel<BudgetsEffect>()
    val effect = _effect.receiveAsFlow()

    val state: StateFlow<BudgetsState> = combine(
        getBudgets(userId, today.year, today.monthNumber),
        getCategories(userId),
        _showDialog
    ) { budgets, categories, showDialog ->
        BudgetsState(
            budgets       = budgets,
            categories    = categories,
            isLoading     = false,
            showAddDialog = showDialog
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsState(isLoading = true)
    )

    fun onIntent(intent: BudgetsIntent) {
        when (intent) {
            BudgetsIntent.Refresh        -> { /* flows auto-refresh */ }
            BudgetsIntent.AddBudgetClick -> _showDialog.update { true }
            BudgetsIntent.DismissDialog  -> _showDialog.update { false }
            is BudgetsIntent.ConfirmAddBudget -> addNewBudget(intent.category, intent.limit)
            is BudgetsIntent.DeleteBudget     -> removeBudget(intent.id)
        }
    }

    private fun addNewBudget(category: Category, limit: Double) {
        viewModelScope.launch {
            try {
                addBudget(
                    Budget(
                        id          = uuid4().toString(),
                        category    = category,
                        limitAmount = limit,
                        spent       = 0.0,
                        period      = BudgetPeriod.MONTHLY,
                        userId      = userId,
                        isDirty     = true
                    )
                )
                _showDialog.update { false }
                _effect.send(BudgetsEffect.BudgetAdded)
            } catch (e: Exception) {
                _effect.send(BudgetsEffect.Error(e.message ?: "Failed to add budget"))
            }
        }
    }

    private fun removeBudget(id: String) {
        viewModelScope.launch {
            try {
                deleteBudget(id)
                _effect.send(BudgetsEffect.BudgetDeleted)
            } catch (e: Exception) {
                _effect.send(BudgetsEffect.Error(e.message ?: "Failed to delete budget"))
            }
        }
    }
}
