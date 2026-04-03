package com.ryuken.obsidianledger.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.usecase.SyncUseCase
import com.ryuken.obsidianledger.features.dashboard.GetProfileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfile  : GetProfileUseCase,
    private val signOut     : SignOutUseCase,
    private val exportCsv   : ExportCsvUseCase,
    private val syncUseCase : SyncUseCase,
    private val authRepo    : AuthRepository
) : ViewModel() {

    private val userId = authRepo.currentUserId() ?: ""

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ProfileEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Refresh   -> loadProfile()
            ProfileIntent.ExportCsv -> exportData()
            ProfileIntent.SyncNow   -> syncData()
            ProfileIntent.SignOut    -> performSignOut()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val profile = getProfile(userId)
                _state.update {
                    it.copy(
                        profile   = profile,
                        isLoading = false,
                        error     = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error     = e.message
                    )
                }
            }
        }
    }

    private fun exportData() {
        viewModelScope.launch {
            try {
                val csv = exportCsv(userId)
                _effect.send(ProfileEffect.CsvExported(csv))
            } catch (e: Exception) {
                _effect.send(ProfileEffect.Error(e.message ?: "Export failed"))
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            try {
                syncUseCase(userId)
                _state.update { it.copy(lastSyncTimestamp = "Just now") }
                _effect.send(ProfileEffect.SyncComplete)
            } catch (e: Exception) {
                _effect.send(ProfileEffect.Error(e.message ?: "Sync failed"))
            }
        }
    }

    private fun performSignOut() {
        viewModelScope.launch {
            try {
                signOut()
                _effect.send(ProfileEffect.SignedOut)
            } catch (e: Exception) {
                _effect.send(ProfileEffect.Error(e.message ?: "Sign out failed"))
            }
        }
    }
}
