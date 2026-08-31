package com.ryuken.obsidianledger

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.repository.AuthSessionState
import com.ryuken.obsidianledger.core.ui.components.AUTH_RESOLVE_TIMEOUT_MS
import com.ryuken.obsidianledger.core.ui.components.SPLASH_MIN_DURATION_MS
import com.ryuken.obsidianledger.core.ui.components.SplashScreen
import com.ryuken.obsidianledger.core.ui.theme.AppTheme
import com.ryuken.obsidianledger.features.auth.AuthScreen
import com.ryuken.obsidianledger.features.expenses.AddTransactionScreen
import com.ryuken.obsidianledger.features.main.MainScreen
import com.ryuken.obsidianledger.features.splits.AddSplitExpenseScreen
import com.ryuken.obsidianledger.features.splits.AddSplitExpenseViewModel
import com.ryuken.obsidianledger.features.splits.CreateGroupScreen
import com.ryuken.obsidianledger.features.splits.GroupDetailScreen
import com.ryuken.obsidianledger.features.splits.GroupDetailViewModel
import com.ryuken.obsidianledger.navigation.RootComponent
import com.ryuken.obsidianledger.navigation.RootComponent.Child
import com.ryuken.obsidianledger.core.preferences.AppPreferences
import com.ryuken.obsidianledger.core.domain.usecase.SyncUseCase
import com.ryuken.obsidianledger.core.ui.theme.LedgerThemeConfig
import com.ryuken.obsidianledger.core.ui.theme.LedgerCurrencyConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.LaunchedEffect
import org.koin.core.parameter.parametersOf

@Composable
fun App(
    root: RootComponent,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) {
    val appPrefs = koinInject<AppPreferences>()
    val authRepo = koinInject<AuthRepository>()

    // RootComponent's initial Auth-vs-Main choice is a synchronous best guess made
    // before the session finishes loading from encrypted storage — on cold start it's
    // wrong just often enough to flash the login screen. Block first render behind the
    // splash until the real auth state resolves, then route once and reveal. The splash
    // also stays up for a guaranteed minimum so the dashboard's first Supabase fetches
    // (profile, transactions, budgets, groups) get a head start instead of landing on an
    // empty/loading Main screen right after the flash-free reveal.
    var isResolvingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        LedgerThemeConfig.themeFlow.value = appPrefs.getString(AppPreferences.KEY_THEME, "System")

        val cur = appPrefs.getString(AppPreferences.KEY_CURRENCY, "INR (₹)")
        val symbol = cur.substringAfter("(").removeSuffix(")")
        LedgerCurrencyConfig.currencyFlow.value = if (symbol.isNotEmpty() && symbol != cur) symbol else "₹"
    }

    LaunchedEffect(Unit) {
        val resolvedAuthState = async {
            runCatching { authRepo.observeAuthState().first { it !is AuthSessionState.Initializing } }
                .onFailure { Napier.e("Auth state resolution failed; routing to sign-in", it) }
                .getOrElse { AuthSessionState.NotAuthenticated }
        }
        delay(SPLASH_MIN_DURATION_MS)
        // A stuck Initializing flow (storage read wedged, session refresh hanging) must
        // not hold the splash hostage forever: route to sign-in after a hard timeout.
        // Session restore reads local encrypted storage first, so a signed-in user
        // resolves in milliseconds — only a genuinely broken session hits the timeout.
        val resolved = withTimeoutOrNull(AUTH_RESOLVE_TIMEOUT_MS) { resolvedAuthState.await() }
        if (resolved == null) {
            Napier.e("Auth state did not resolve within ${AUTH_RESOLVE_TIMEOUT_MS}ms; routing to sign-in")
        }
        when (startupRoute(resolved)) {
            StartupRoute.Main -> if (root.stack.value.active.configuration !is RootComponent.Config.Main) root.replaceWithMain()
            StartupRoute.Auth -> if (root.stack.value.active.configuration !is RootComponent.Config.Auth) root.replaceWithAuth()
        }
        isResolvingAuth = false
    }

    // Bidirectional sync on every (re)authentication: pulls cloud data into a fresh
    // install / second device immediately instead of waiting for the WorkManager tick
    // or a manual SyncNow. Fire-and-forget — the UI reads local SQLDelight and fills
    // in reactively as rows land; failures are logged and picked up by the next tick.
    val syncUseCase = koinInject<SyncUseCase>()
    LaunchedEffect(Unit) {
        authRepo.observeAuthState()
            .filterIsInstance<AuthSessionState.Authenticated>()
            .distinctUntilChangedBy { it.userId }
            .collect { state ->
                runCatching { syncUseCase(state.userId) }
                    .onFailure { Napier.e("Sign-in sync failed; retrying on next scheduled tick", it) }
            }
    }

    AppTheme(onThemeChanged = onThemeChanged) {
        Box(modifier = Modifier.fillMaxSize()) {
            RootNavHost(root = root)
            if (isResolvingAuth) {
                SplashScreen()
            }
        }
    }
}

/**
 * Pure startup routing decision given the (possibly timed-out or failed) auth state.
 * Anything that is not a confirmed Authenticated session routes to sign-in — the
 * recoverable fallback: the user can retry, and a transient session glitch costs one
 * extra login instead of a frozen splash.
 */
internal enum class StartupRoute { Main, Auth }

internal fun startupRoute(resolved: AuthSessionState?): StartupRoute =
    if (resolved is AuthSessionState.Authenticated) StartupRoute.Main else StartupRoute.Auth

@OptIn(com.arkivanov.decompose.FaultyDecomposeApi::class)
@Composable
private fun RootNavHost(root: RootComponent) {
    val stack by root.stack.subscribeAsState()

    Children(
        stack     = stack,
        animation = stackAnimation { _, _, direction ->
            if (direction == Direction.ENTER_FRONT) {
                // Push — fade in with a slight scale-up bounce
                fade(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)) +
                scale(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
            } else {
                // Pop — just a clean fade
                fade(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
            }
        }
    ) { child ->
        when (val instance = child.instance) {
            is Child.Auth           -> AuthScreen(
                onSuccess = { root.replaceWithMain() }
            )
            is Child.Main           -> MainScreen(
                component        = instance.component,
                onAddTransaction = { root.navigateTo(RootComponent.Config.AddTransaction) },
                onSignedOut      = { root.replaceWithAuth() },
                onNavigateToGroup = { groupId ->
                    root.navigateTo(RootComponent.Config.GroupDetail(groupId))
                },
                onCreateGroup = {
                    root.navigateTo(RootComponent.Config.CreateGroup)
                }
            )
            is Child.AddTransaction -> AddTransactionScreen(
                onBack = { root.pop() }
            )
            is Child.CreateGroup    -> CreateGroupScreen(
                onBack    = { root.pop() },
                onCreated = { root.pop() }
            )
            is Child.GroupDetail    -> {
                val vm: GroupDetailViewModel = koinViewModel(
                    parameters = { parametersOf(instance.groupId) }
                )
                GroupDetailScreen(
                    viewModel    = vm,
                    onBack       = { root.pop() },
                    onAddExpense = { groupId ->
                        root.navigateTo(RootComponent.Config.AddSplitExpense(groupId))
                    }
                )
            }
            is Child.AddSplitExpense -> {
                val vm: AddSplitExpenseViewModel = koinViewModel(
                    parameters = { parametersOf(instance.groupId) }
                )
                AddSplitExpenseScreen(
                    viewModel = vm,
                    onBack    = { root.pop() }
                )
            }
        }
    }
}
