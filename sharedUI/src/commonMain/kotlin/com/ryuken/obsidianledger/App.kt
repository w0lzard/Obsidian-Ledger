package com.ryuken.obsidianledger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ryuken.obsidianledger.core.ui.theme.ObsidianLedgerTheme
import com.ryuken.obsidianledger.feature.auth.AuthScreen
import com.ryuken.obsidianledger.feature.expenses.AddTransactionScreen
import com.ryuken.obsidianledger.feature.main.MainScreen
import com.ryuken.obsidianledger.navigation.RootComponent
import com.ryuken.obsidianledger.navigation.RootComponent.Child
import org.koin.compose.KoinContext

@Composable
fun App(root: RootComponent) {
    KoinContext {
        ObsidianLedgerTheme {
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
                onAddTransaction = { root.navigateTo(RootComponent.Config.AddTransaction) }
            )
            is Child.AddTransaction -> AddTransactionScreen(
                component = instance.component,
                onBack    = { root.pop() }
            )
        }
    }
}