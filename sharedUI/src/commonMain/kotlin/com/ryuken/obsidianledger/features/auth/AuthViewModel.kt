package com.ryuken.obsidianledger.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>()
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.TabChanged         -> _state.update { it.copy(activeTab = intent.tab, error = null) }
            is AuthIntent.EmailChanged       -> _state.update { it.copy(email = intent.email, error = null) }
            is AuthIntent.PasswordChanged    -> _state.update { it.copy(password = intent.password, error = null) }
            is AuthIntent.DisplayNameChanged -> _state.update { it.copy(displayName = intent.name, error = null) }
            AuthIntent.SubmitClick           -> submit()
        }
    }

    private fun submit() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "Email and password are required") }
            return
        }
        if (s.activeTab == AuthTab.CREATE_ACCOUNT && s.displayName.isBlank()) {
            _state.update { it.copy(error = "Display name is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                when (s.activeTab) {
                    AuthTab.SIGN_IN -> signIn(s.email, s.password)
                    AuthTab.CREATE_ACCOUNT -> signUp(s.email, s.password, s.displayName)
                }
                _effect.send(AuthEffect.AuthSuccess)
            } catch (e: Exception) {
                val message = e.message ?: "Authentication failed"
                _state.update { it.copy(error = message) }
                _effect.send(AuthEffect.Error(message))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
