package com.ryuken.obsidianledger

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
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
import com.ryuken.obsidianledger.core.ui.theme.LedgerThemeConfig
import com.ryuken.obsidianledger.core.ui.theme.LedgerCurrencyConfig
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.LaunchedEffect
import org.koin.core.parameter.parametersOf

@Composable
fun App(
    root: RootComponent,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) {
    KoinContext {
        val appPrefs = koinInject<AppPreferences>()
        
        LaunchedEffect(Unit) {
            LedgerThemeConfig.themeFlow.value = appPrefs.getString(AppPreferences.KEY_THEME, "System")
            
            val cur = appPrefs.getString(AppPreferences.KEY_CURRENCY, "INR (₹)")
            val symbol = cur.substringAfter("(").removeSuffix(")")
            LedgerCurrencyConfig.currencyFlow.value = if (symbol.isNotEmpty() && symbol != cur) symbol else "₹"
        }

        AppTheme(onThemeChanged = onThemeChanged) {
            RootNavHost(root = root)
        }
    }
}

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
