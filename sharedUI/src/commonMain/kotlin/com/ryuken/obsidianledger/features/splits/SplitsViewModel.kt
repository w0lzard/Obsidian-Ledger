package com.ryuken.obsidianledger.features.splits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import com.ryuken.obsidianledger.core.domain.usecase.GetGroupsUseCase
import com.ryuken.obsidianledger.features.dashboard.GetProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SplitsViewModel(
    private val getGroups  : GetGroupsUseCase,
    private val splitRepo  : SplitRepository,
    private val getProfile : GetProfileUseCase,
    private val authRepo   : AuthRepository
) : ViewModel() {

    // Reactive so a ViewModel created before session restore finishes doesn't freeze
    // on a null/empty userId for its whole lifetime.
    private val userId: StateFlow<String?> = authRepo.observeUserId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepo.currentUserId())

    private val _refreshTick = MutableStateFlow(0)
    private val _isCreating  = MutableStateFlow(false)

    // Buffered so an effect sent before the UI collector attaches isn't dropped
    // by the default rendezvous Channel.
    private val _effect = Channel<SplitsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val state: StateFlow<SplitsState> = userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(SplitsState(isLoading = true))
        } else {
            combine(
                _refreshTick.flatMapLatest { getGroups(uid) },
                _isCreating
            ) { groups, isCreating ->
                SplitsState(groups = groups, isLoading = false, isCreatingGroup = isCreating)
            }
        }
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SplitsState(isLoading = true)
    )

    fun onIntent(intent: SplitsIntent) {
        when (intent) {
            SplitsIntent.Refresh          -> _refreshTick.update { it + 1 }
            is SplitsIntent.CreateGroup   -> createGroup(intent.name, intent.memberNames)
        }
    }

    private fun createGroup(name: String, memberNames: List<String>) {
        if (_isCreating.value) return
        val uid = userId.value ?: return

        viewModelScope.launch {
            _isCreating.update { true }
            try {
                val creatorName = runCatching { getProfile(uid) }
                    .getOrNull()?.displayName?.ifBlank { "You" } ?: "You"
                val group = splitRepo.createGroup(name, uid, creatorName, memberNames)
                _refreshTick.update { it + 1 }
                _effect.send(SplitsEffect.GroupCreated(group.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(SplitsEffect.Error(e.message ?: "Failed to create group"))
            } finally {
                _isCreating.update { false }
            }
        }
    }
}
