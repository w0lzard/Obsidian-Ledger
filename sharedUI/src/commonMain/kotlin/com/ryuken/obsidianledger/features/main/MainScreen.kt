package com.ryuken.obsidianledger.features.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.features.analytics.AnalyticsScreen
import com.ryuken.obsidianledger.features.budgets.BudgetsScreen
import com.ryuken.obsidianledger.features.dashboard.DashboardScreen
import com.ryuken.obsidianledger.features.profile.ProfileScreen
import com.ryuken.obsidianledger.features.splits.SplitsScreen
import com.ryuken.obsidianledger.navigation.MainComponent

@Composable
fun MainScreen(
    component        : MainComponent,
    onAddTransaction : () -> Unit,
    onSignedOut      : () -> Unit = {},
    onNavigateToGroup: (String) -> Unit = {},
    onCreateGroup    : () -> Unit = {}
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
            animation = stackAnimation(
                fade(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            ),
            modifier  = Modifier.padding(padding)
        ) { child ->
            when (child.instance) {
                is MainComponent.Child.Dashboard -> DashboardScreen(
                    onAddTransaction = onAddTransaction,
                    onSplitsClick    = { component.navigateTo(MainComponent.Config.Splits) }
                )
                is MainComponent.Child.Analytics -> AnalyticsScreen()
                is MainComponent.Child.Splits    -> SplitsScreen(
                    onNavigateToGroup = onNavigateToGroup,
                    onCreateGroup     = onCreateGroup
                )
                is MainComponent.Child.Budgets   -> BudgetsScreen()
                is MainComponent.Child.Profile   -> ProfileScreen(
                    onSignedOut = onSignedOut
                )
            }
        }
    }
}