package com.ryuken.obsidianledger.features.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class AddTransactionViewModel(
    private val addTransaction: AddTransactionUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val userId = authRepo.currentUserId() ?: ""

    private val _state = MutableStateFlow(AddTransactionState())
    val state = _state.asStateFlow()

    private val _effect = Channel<AddTransactionEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            getCategories(userId).collectLatest { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    fun onIntent(intent: AddTransactionIntent) {
        when (intent) {
            is AddTransactionIntent.NumpadInput -> handleNumpad(intent.key)
            AddTransactionIntent.NumpadDelete -> _state.update {
                it.copy(amount = it.amount.dropLast(1))
            }
            is AddTransactionIntent.TypeChanged -> _state.update { it.copy(type = intent.type) }
            is AddTransactionIntent.CategorySelected -> _state.update {
                it.copy(selectedCategory = intent.category)
            }
            is AddTransactionIntent.NoteChanged -> _state.update { it.copy(note = intent.note) }
            is AddTransactionIntent.DateChanged -> _state.update { it.copy(date = intent.date) }
            AddTransactionIntent.SaveClick -> save()
        }
    }

    private fun handleNumpad(key: String) {
        val current = _state.value.amount
        if (key == "." && "." in current) return
        if (current.length >= 9) return
        _state.update { it.copy(amount = current + key) }
    }

    private fun save() {
        val currentState = _state.value
        if (!currentState.canSave) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val now = Clock.System.now()
                val transaction = Transaction(
                    id = uuid4().toString(),
                    amount = currentState.amountDouble,
                    type = currentState.type,
                    category = currentState.selectedCategory!!,
                    note = currentState.note.takeIf { it.isNotBlank() },
                    date = currentState.date,
                    createdAt = now,
                    updatedAt = now,
                    isDirty = true,
                    userId = userId
                )
                addTransaction(transaction)
                _effect.send(AddTransactionEffect.SaveSuccess)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
                _effect.send(AddTransactionEffect.Error(e.message ?: "Unknown error"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
