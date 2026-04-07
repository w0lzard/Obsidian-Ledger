package com.ryuken.obsidianledger.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import com.ryuken.obsidianledger.core.domain.usecase.SyncUseCase
import com.ryuken.obsidianledger.features.dashboard.GetProfileUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

import com.ryuken.obsidianledger.core.preferences.AppPreferences
import kotlin.time.Clock

class ProfileViewModel(
    private val getProfile  : GetProfileUseCase,
    private val signOut     : SignOutUseCase,
    private val exportCsv   : ExportCsvUseCase,
    private val syncUseCase : SyncUseCase,
    private val authRepo    : AuthRepository,
    private val transactionRepo : TransactionRepository,
    private val budgetRepo      : BudgetRepository,
    private val profileRepo : com.ryuken.obsidianledger.core.domain.repository.ProfileRepository,
    private val appPrefs    : AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ProfileEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        _state.update {
            it.copy(
                currency             = appPrefs.getString(AppPreferences.KEY_CURRENCY, "INR (₹)"),
                theme                = appPrefs.getString(AppPreferences.KEY_THEME, "System"),
                notificationsEnabled = appPrefs.getBoolean(AppPreferences.KEY_NOTIFICATIONS, true),
                lastSyncTimestamp    = appPrefs.getString("last_sync", "Never")
            )
        }

        // Try loading immediately if session is already available
        val immediateUserId = authRepo.currentUserId()
        if (immediateUserId != null) {
            Napier.d("ProfileViewModel: immediate userId available — loading profile")
            loadProfile(immediateUserId)
        }

        // Also observe session changes to handle delayed restoration
        viewModelScope.launch {
            authRepo.observeUserId()
                .filterNotNull()
                .distinctUntilChanged()
                .collect { userId ->
                    Napier.d("ProfileViewModel: observeUserId emitted $userId")
                    // Only reload if not already loaded successfully
                    if (_state.value.isLoading || _state.value.error != null) {
                        loadProfile(userId)
                    }
                    observeStats(userId)
                }
        }

        // If we already have a userId, also start stats immediately
        if (immediateUserId != null) {
            observeStats(immediateUserId)
        }

        // Show not signed in only after waiting for session restoration
        viewModelScope.launch {
            // Wait 5 seconds for session to restore before giving up
            delay(5000L)
            if (_state.value.isLoading) {
                Napier.w("ProfileViewModel: timed out waiting for userId")
                _state.update { it.copy(
                    isLoading = false,
                    error     = "Not signed in"
                )}
            }
        }
    }

    fun retry() {
        Napier.d("ProfileViewModel: retry called")
        _state.update { it.copy(isLoading = true, error = null) }
        val userId = authRepo.currentUserId()
        if (userId != null) {
            loadProfile(userId)
        } else {
            viewModelScope.launch {
                authRepo.observeUserId()
                    .filterNotNull()
                    .take(1)
                    .collect { id ->
                        loadProfile(id)
                    }
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Refresh   -> {
                val userId = authRepo.currentUserId()
                if (userId != null) loadProfile(userId)
            }
            ProfileIntent.ExportCsv -> exportData()
            ProfileIntent.SyncNow   -> syncData()
            ProfileIntent.SignOut    -> performSignOut()
            
            is ProfileIntent.ToggleCurrencyDialog       -> _state.update { it.copy(isCurrencyDialogOpen = intent.open) }
            is ProfileIntent.ToggleThemeDialog          -> _state.update { it.copy(isThemeDialogOpen = intent.open) }
            is ProfileIntent.ToggleEditProfileDialog    -> _state.update { it.copy(isEditProfileDialogOpen = intent.open) }
            is ProfileIntent.ToggleChangePasswordDialog -> _state.update { it.copy(isChangePasswordDialogOpen = intent.open) }
            is ProfileIntent.ToggleImportDialog         -> _state.update { it.copy(isImportDialogOpen = intent.open) }
            
            is ProfileIntent.SetCurrency -> {
                appPrefs.putString(AppPreferences.KEY_CURRENCY, intent.currency)
                val symbol = intent.currency.substringAfter("(").removeSuffix(")")
                com.ryuken.obsidianledger.core.ui.theme.LedgerCurrencyConfig.currencyFlow.value = if (symbol.isNotEmpty() && symbol != intent.currency) symbol else "₹"
                _state.update { it.copy(currency = intent.currency, isCurrencyDialogOpen = false) }
            }
            is ProfileIntent.SetTheme -> {
                appPrefs.putString(AppPreferences.KEY_THEME, intent.theme)
                com.ryuken.obsidianledger.core.ui.theme.LedgerThemeConfig.themeFlow.value = intent.theme
                _state.update { it.copy(theme = intent.theme, isThemeDialogOpen = false) }
            }
            is ProfileIntent.ToggleNotifications -> {
                appPrefs.putBoolean(AppPreferences.KEY_NOTIFICATIONS, intent.enabled)
                _state.update { it.copy(notificationsEnabled = intent.enabled) }
            }
            is ProfileIntent.UpdateDisplayName -> updateDisplayName(intent.name)
            is ProfileIntent.UpdatePassword -> updatePassword(intent.pass)
        }
    }

    private fun loadProfile(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val profile = getProfile(userId)
                Napier.d("Profile loaded: ${profile.displayName}")
                val memberSince = formatMemberSince(profile.createdAt)
                _state.update {
                    it.copy(
                        profile     = profile,
                        memberSince = memberSince,
                        isLoading   = false,
                        error       = null
                    )
                }
            } catch (e: Exception) {
                Napier.e("Profile load failed: ${e.message}", e)
                val message = when {
                    e.message?.contains("request timeout", ignoreCase = true) == true ||
                        e.message?.contains("request_timeout", ignoreCase = true) == true ||
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Request timeout has expired. Please check your internet and retry."
                    else -> e.message ?: "Failed to load profile"
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        error     = "$message (ID: $userId)"
                    )
                }
            }
        }
    }

    private var statsJobStartedFor: String? = null
    private fun observeStats(userId: String) {
        if (statsJobStartedFor == userId) return
        statsJobStartedFor = userId

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val year = today.year
        val month = today.monthNumber

        viewModelScope.launch {
            transactionRepo.observeByMonth(userId = userId, year = year, month = month)
                .collect { txs ->
                    _state.update { it.copy(transactionCount = txs.size) }
                }
        }

        viewModelScope.launch {
            budgetRepo.observeBudgetsWithSpending(userId = userId, year = year, month = month)
                .collect { budgets ->
                    _state.update { it.copy(activeBudgets = budgets.size) }
                }
        }
    }

    private fun exportData() {
        val userId = authRepo.currentUserId() ?: return
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
        val userId = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            try {
                syncUseCase(userId)
                
                val nowStr = "Just now"
                appPrefs.putString("last_sync", nowStr)
                _state.update { it.copy(lastSyncTimestamp = nowStr) }
                
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

    private fun formatMemberSince(createdAt: String?): String {
        if (createdAt == null) return "—"
        return try {
            val instant = Instant.parse(createdAt)
            val date = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} ${date.year}"
        } catch (e: Exception) {
            "—"
        }
    }

    private fun updateDisplayName(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isAuthActionLoading = true) }
            try {
                authRepo.updateUser(name)
                val userId = authRepo.currentUserId()
                if (userId != null) {
                    profileRepo.updateProfile(userId, name)
                }
                val currentProfile = _state.value.profile
                if (currentProfile != null) {
                    _state.update { it.copy(profile = currentProfile.copy(displayName = name)) }
                }
                _state.update { it.copy(isEditProfileDialogOpen = false, error = null) }
                _effect.send(ProfileEffect.ShowMessage("Profile updated successfully"))
            } catch (e: Exception) {
                _effect.send(ProfileEffect.Error(e.message ?: "Failed to update profile"))
            } finally {
                _state.update { it.copy(isAuthActionLoading = false) }
            }
        }
    }

    private fun updatePassword(pass: String) {
        viewModelScope.launch {
            _state.update { it.copy(isAuthActionLoading = true) }
            try {
                authRepo.updatePassword(pass)
                _state.update { it.copy(isChangePasswordDialogOpen = false, error = null) }
                _effect.send(ProfileEffect.ShowMessage("Password updated successfully"))
            } catch (e: Exception) {
                _effect.send(ProfileEffect.Error(e.message ?: "Failed to update password"))
            } finally {
                _state.update { it.copy(isAuthActionLoading = false) }
            }
        }
    }
}
