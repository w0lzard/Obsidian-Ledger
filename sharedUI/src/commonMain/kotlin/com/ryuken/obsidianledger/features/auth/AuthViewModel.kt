package com.ryuken.obsidianledger.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryuken.obsidianledger.core.auth.SupabaseSessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.distinctUntilChanged

class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        // Navigate to Main when session status becomes authenticated
        viewModelScope.launch {
            supabaseClient.auth.sessionStatus
                .collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            io.github.aakira.napier.Napier.d("AuthViewModel: already authenticated — navigating to Main")
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(AuthEffect.AuthSuccess)
                        }
                        is SessionStatus.Initializing -> {
                            io.github.aakira.napier.Napier.d("AuthViewModel: initializing session")
                            _state.update { it.copy(isLoading = true) }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            io.github.aakira.napier.Napier.d("AuthViewModel: not authenticated — showing auth screen")
                            _state.update { it.copy(isLoading = false) }
                        }
                        is SessionStatus.RefreshFailure -> {
                            io.github.aakira.napier.Napier.e("AuthViewModel: refresh failure checking session")
                            _state.update { it.copy(isLoading = false) }
                        }
                        else -> {
                            _state.update { it.copy(isLoading = false) }
                        }
                    }
                }
        }
        
        // Setup existing observer for OAuth deep link
        viewModelScope.launch {
            com.ryuken.obsidianledger.core.auth.SupabaseSessionManager.sessionEstablished.collect {
                _state.update { it.copy(isLoading = false) }
                _effect.send(AuthEffect.AuthSuccess)
            }
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.TabChanged         -> _state.update { it.copy(activeTab = intent.tab, error = null) }
            is AuthIntent.EmailChanged       -> _state.update { it.copy(email = intent.email, error = null) }
            is AuthIntent.PasswordChanged    -> _state.update { it.copy(password = intent.password, error = null) }
            is AuthIntent.DisplayNameChanged -> _state.update { it.copy(displayName = intent.name, error = null) }
            AuthIntent.SubmitClick           -> submit()
            AuthIntent.GoogleSignInClick     -> onGoogleSignIn()
        }
    }

    private fun onGoogleSignIn() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                signInWithGoogle(redirectTo = "obsidianledger://auth/callback")
                // Success is handled by deep link interceptor in AppActivity which sets the session
            } catch (e: Exception) {
                val message = e.message ?: "Google Sign-in failed"
                _state.update { it.copy(error = message) }
                _effect.send(AuthEffect.Error(message))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
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
