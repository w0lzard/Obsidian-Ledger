package com.ryuken.obsidianledger.features.splits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.helper.distributeCentsEvenly
import com.ryuken.obsidianledger.core.domain.helper.toCents
import com.ryuken.obsidianledger.core.domain.model.SplitShare
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import com.ryuken.obsidianledger.core.domain.usecase.AddSplitExpenseUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class AddSplitExpenseViewModel(
    private val groupId: String,
    private val splitRepo: SplitRepository,
    private val addExpense: AddSplitExpenseUseCase,
    private val authRepo: AuthRepository
) : ViewModel() {

    // Reactive so a ViewModel created before session restore finishes doesn't freeze
    // on a null/empty userId for its whole lifetime.
    private val userId: StateFlow<String?> = authRepo.observeUserId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepo.currentUserId())

    private val _state = MutableStateFlow(AddSplitExpenseState())
    val state = _state.asStateFlow()

    // Buffered so an effect sent before the UI collector attaches isn't dropped
    // by the default rendezvous Channel.
    private val _effect = Channel<AddSplitExpenseEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            splitRepo.observeGroup(groupId).collectLatest { group ->
                val uid = userId.value
                _state.update {
                    it.copy(
                        group = group,
                        isLoading = false,
                        payerMemberId = it.payerMemberId
                            ?: group.members.firstOrNull { m -> m.userId == uid }?.id
                            ?: group.members.firstOrNull()?.id,
                        participantMemberIds = it.participantMemberIds.ifEmpty {
                            group.members.map { m -> m.id }.toSet()
                        }
                    )
                }
            }
        }
    }

    fun onIntent(intent: AddSplitExpenseIntent) {
        when (intent) {
            is AddSplitExpenseIntent.DescriptionChanged -> _state.update { it.copy(description = intent.value) }
            is AddSplitExpenseIntent.AmountChanged      -> _state.update { it.copy(amount = intent.value) }
            is AddSplitExpenseIntent.PayerSelected      -> _state.update { it.copy(payerMemberId = intent.memberId) }
            is AddSplitExpenseIntent.ToggleParticipant  -> _state.update {
                val current = it.participantMemberIds
                it.copy(
                    participantMemberIds = if (intent.memberId in current) current - intent.memberId
                                            else current + intent.memberId
                )
            }
            AddSplitExpenseIntent.SaveClick -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.canSave) return
        val payerId = s.payerMemberId ?: return
        val group = s.group ?: return
        val uid = userId.value ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val payer = group.members.firstOrNull { it.id == payerId }
                addExpense(
                    groupId            = groupId,
                    description        = s.description.trim(),
                    amount             = s.amountDouble,
                    paidByMemberId     = payerId,
                    date               = today,
                    shares             = equalShares(s.amountDouble, s.participantMemberIds.toList()),
                    currentUserId      = uid,
                    payerIsCurrentUser = payer?.userId == uid
                )
                _effect.send(AddSplitExpenseEffect.SaveSuccess)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(AddSplitExpenseEffect.Error(e.message ?: "Failed to add expense"))
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun equalShares(amount: Double, memberIds: List<String>): List<SplitShare> {
        val cents = distributeCentsEvenly(amount.toCents(), memberIds.size)
        return memberIds.mapIndexed { index, id -> SplitShare(memberId = id, amount = cents[index] / 100.0) }
    }
}
