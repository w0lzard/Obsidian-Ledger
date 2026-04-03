package com.ryuken.obsidianledger.features.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.features.analytics.AnalyticsScreen
import com.ryuken.obsidianledger.features.budgets.BudgetsScreen
import com.ryuken.obsidianledger.features.dashboard.DashboardScreen
import com.ryuken.obsidianledger.features.profile.ProfileScreen
import com.ryuken.obsidianledger.navigation.MainComponent

@Composable
fun MainScreen(
    component        : MainComponent,
    onAddTransaction : () -> Unit,
    onSignedOut      : () -> Unit = {}
) {
    val stack by component.stack.subscribeAsState()
    val colors = LedgerTheme.colors

    Scaffold(
        containerColor = colors.surfaceBase,
        bottomBar = {
            LedgerBottomNav(
                currentConfig = stack.active.configuration,
                onTabSelected = { component.navigateTo(it) }
            )
        }
    ) { padding ->
        Children(
            stack     = stack,
            animation = stackAnimation(fade()),
            modifier  = Modifier.padding(padding)
        ) { child ->
            when (child.instance) {
                is MainComponent.Child.Dashboard -> DashboardScreen(
                    onAddTransaction = onAddTransaction
                )
                is MainComponent.Child.Analytics -> AnalyticsScreen()
                is MainComponent.Child.Budgets   -> BudgetsScreen()
                is MainComponent.Child.Profile   -> ProfileScreen(
                    onSignedOut = onSignedOut
                )
            }
        }
    }
}