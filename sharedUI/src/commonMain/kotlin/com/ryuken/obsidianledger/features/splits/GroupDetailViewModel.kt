package com.ryuken.obsidianledger.features.splits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import com.ryuken.obsidianledger.core.domain.usecase.EditMemberUseCase
import com.ryuken.obsidianledger.core.domain.usecase.GetGroupBalancesUseCase
import com.ryuken.obsidianledger.core.domain.usecase.RecordSettlementUseCase
import com.ryuken.obsidianledger.core.domain.usecase.RemoveMemberUseCase
import com.ryuken.obsidianledger.core.domain.usecase.SendPaymentRequestUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class GroupDetailViewModel(
    private val groupId: String,
    private val splitRepo: SplitRepository,
    private val recordSettlement: RecordSettlementUseCase,
    private val getBalances: GetGroupBalancesUseCase,
    private val sendPaymentRequest: SendPaymentRequestUseCase,
    private val authRepo: AuthRepository,
    private val editMemberUseCase: EditMemberUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase
) : ViewModel() {

    // Reactive so a ViewModel created before session restore finishes doesn't freeze
    // on a null/empty userId for its whole lifetime.
    private val userId: StateFlow<String?> = authRepo.observeUserId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepo.currentUserId())
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private val _refreshTick = MutableStateFlow(0)
    private val _editingMemberId = MutableStateFlow<String?>(null)
    private val _settlingWithMemberId = MutableStateFlow<String?>(null)
    private val _sendingRequestForMemberId = MutableStateFlow<String?>(null)

    // Buffered so an effect sent before the UI collector attaches isn't dropped
    // by the default rendezvous Channel.
    private val _effect = Channel<GroupDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val state: StateFlow<GroupDetailState> = userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(GroupDetailState(isLoading = true))
        } else {
            combine(
                _refreshTick.flatMapLatest { splitRepo.observeGroup(groupId) },
                _refreshTick.flatMapLatest { getBalances(groupId) },
                _refreshTick.flatMapLatest { splitRepo.observeExpenses(groupId) },
                _editingMemberId,
                _settlingWithMemberId,
                _sendingRequestForMemberId
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val group = args[0] as com.ryuken.obsidianledger.core.domain.model.SplitGroup
                @Suppress("UNCHECKED_CAST") val balances = args[1] as List<com.ryuken.obsidianledger.core.domain.model.MemberBalance>
                @Suppress("UNCHECKED_CAST") val expenses = args[2] as List<com.ryuken.obsidianledger.core.domain.model.SplitExpense>
                val editingId = args[3] as String?
                val settlingId = args[4] as String?
                val sendingId = args[5] as String?

                GroupDetailState(
                    group             = group,
                    balances          = balances,
                    expenses          = expenses,
                    currentUserId     = uid,
                    isLoading         = false,
                    editingMember     = group.members.firstOrNull { it.id == editingId },
                    settlingWithMemberId = settlingId,
                    sendingRequestForMemberId = sendingId
                )
            }
        }
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupDetailState(isLoading = true)
    )

    fun onIntent(intent: GroupDetailIntent) {
        when (intent) {
            GroupDetailIntent.Refresh                    -> _refreshTick.update { it + 1 }
            is GroupDetailIntent.EditMemberClick          -> _editingMemberId.update { intent.memberId }
            GroupDetailIntent.DismissEditMember           -> _editingMemberId.update { null }
            is GroupDetailIntent.ConfirmEditMember        -> confirmEditMember(intent.displayName)
            is GroupDetailIntent.RemoveMember             -> removeMember(intent.memberId)
            is GroupDetailIntent.SettleUpClick            -> _settlingWithMemberId.update { intent.memberId }
            GroupDetailIntent.DismissSettleUp             -> _settlingWithMemberId.update { null }
            is GroupDetailIntent.ConfirmSettleUp          -> confirmSettleUp(intent.counterpartyMemberId, intent.amount, intent.iPaid)
            is GroupDetailIntent.SendPaymentRequest       -> sendRequest(intent.memberId)
        }
    }

    private fun confirmEditMember(displayName: String) {
        val memberId = _editingMemberId.value ?: return
        viewModelScope.launch {
            try {
                editMemberUseCase(memberId, displayName)
                _editingMemberId.update { null }
                _refreshTick.update { it + 1 }
                _effect.send(GroupDetailEffect.MemberUpdated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(GroupDetailEffect.Error(e.message ?: "Failed to update member"))
            }
        }
    }

    private fun removeMember(memberId: String) {
        viewModelScope.launch {
            try {
                removeMemberUseCase(memberId)
                _refreshTick.update { it + 1 }
                _effect.send(GroupDetailEffect.MemberUpdated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(GroupDetailEffect.Error(e.message ?: "Failed to remove member"))
            }
        }
    }

    private fun confirmSettleUp(counterpartyMemberId: String, amount: Double, iPaid: Boolean) {
        val myMemberId = state.value.currentUserMemberId ?: return
        val uid = userId.value ?: return
        viewModelScope.launch {
            try {
                val fromMemberId = if (iPaid) myMemberId else counterpartyMemberId
                val toMemberId   = if (iPaid) counterpartyMemberId else myMemberId
                recordSettlement(
                    groupId               = groupId,
                    fromMemberId          = fromMemberId,
                    toMemberId            = toMemberId,
                    amount                = amount,
                    date                  = today,
                    currentUserId         = uid,
                    currentUserIsPayer    = iPaid,
                    currentUserIsReceiver = !iPaid
                )
                _settlingWithMemberId.update { null }
                _refreshTick.update { it + 1 }
                _effect.send(GroupDetailEffect.SettlementRecorded)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(GroupDetailEffect.Error(e.message ?: "Failed to record settlement"))
            }
        }
    }

    private fun sendRequest(memberId: String) {
        val current = state.value
        val group = current.group ?: return
        val member = group.members.firstOrNull { it.id == memberId } ?: return
        val email = member.email
        if (email == null) {
            viewModelScope.launch { _effect.send(GroupDetailEffect.Error("No email on file for ${member.displayName}")) }
            return
        }
        val owed = current.balances.firstOrNull { it.memberId == memberId }?.netAmount ?: 0.0
        if (owed >= 0) {
            viewModelScope.launch { _effect.send(GroupDetailEffect.Error("${member.displayName} doesn't owe anything")) }
            return
        }
        val fromUserName = group.members.firstOrNull { it.userId == userId.value }?.displayName ?: "Someone"

        viewModelScope.launch {
            _sendingRequestForMemberId.update { memberId }
            try {
                val result = sendPaymentRequest(
                    toEmail      = email,
                    toName       = member.displayName,
                    fromUserName = fromUserName,
                    amount       = -owed,
                    groupName    = group.name,
                    breakdown    = listOf("Outstanding balance" to -owed)
                )
                result.fold(
                    onSuccess = { _effect.send(GroupDetailEffect.PaymentRequestSent) },
                    onFailure = { e -> _effect.send(GroupDetailEffect.Error(e.message ?: "Failed to send request")) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(GroupDetailEffect.Error(e.message ?: "Failed to send request"))
            } finally {
                _sendingRequestForMemberId.update { null }
            }
        }
    }
}
