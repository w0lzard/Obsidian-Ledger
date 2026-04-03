package com.ryuken.obsidianledger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ryuken.obsidianledger.core.ui.theme.AppTheme
import com.ryuken.obsidianledger.features.auth.AuthScreen
import com.ryuken.obsidianledger.features.expenses.AddTransactionScreen
import com.ryuken.obsidianledger.features.main.MainScreen
import com.ryuken.obsidianledger.navigation.RootComponent
import com.ryuken.obsidianledger.navigation.RootComponent.Child
import org.koin.compose.KoinContext

@Composable
fun App(
    root: RootComponent,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) {
    KoinContext {
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
        animation = stackAnimation(slide())
    ) { child ->
        when (val instance = child.instance) {
            is Child.Auth           -> AuthScreen(
                onSuccess = { root.navigateTo(RootComponent.Config.Main) }
            )
            is Child.Main           -> MainScreen(
                component = instance.component,
                onAddTransaction = { root.navigateTo(RootComponent.Config.AddTransaction) },
                onSignedOut = {
                    // Pop back to auth
                    root.pop()
                }
            )
            is Child.AddTransaction -> AddTransactionScreen(
                onBack = { root.pop() }
            )
        }
    }
}
