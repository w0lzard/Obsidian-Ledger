package com.ryuken.obsidianledger.features.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetPeriod
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.features.expenses.GetCategoriesUseCase
import kotlinx.coroutines.CancellationException
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

    // Reactive so a ViewModel created before session restore finishes doesn't freeze
    // on a null/empty userId for its whole lifetime.
    private val userId: StateFlow<String?> = authRepo.observeUserId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepo.currentUserId())

    private val _showDialog = MutableStateFlow(false)

    // Buffered so an effect sent before the UI collector attaches (e.g. right after
    // a config change) isn't dropped by the default rendezvous Channel.
    private val _effect = Channel<BudgetsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val state: StateFlow<BudgetsState> = userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(BudgetsState(isLoading = true))
        } else {
            combine(
                getBudgets(uid, today.year, today.month.ordinal + 1),
                getCategories(uid),
                _showDialog
            ) { budgets, categories, showDialog ->
                BudgetsState(
                    budgets       = budgets,
                    categories    = categories,
                    isLoading     = false,
                    showAddDialog = showDialog
                )
            }
        }
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
        val uid = userId.value ?: return
        viewModelScope.launch {
            try {
                addBudget(
                    Budget(
                        id          = uuid4().toString(),
                        category    = category,
                        limitAmount = limit,
                        spent       = 0.0,
                        period      = BudgetPeriod.MONTHLY,
                        userId      = uid,
                        isDirty     = true
                    )
                )
                _showDialog.update { false }
                _effect.send(BudgetsEffect.BudgetAdded)
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(BudgetsEffect.Error(e.message ?: "Failed to delete budget"))
            }
        }
    }
}
