package com.ryuken.obsidianledger.features.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.navigation.MainComponent

// ═══════════════════════════════════════════════════════════════════════
// Bottom Navigation Bar — Obsidian Style (no borders, tonal layering)
// ═══════════════════════════════════════════════════════════════════════

private data class NavItem(
    val config          : MainComponent.Config,
    val label           : String,
    val selectedIcon    : ImageVector,
    val unselectedIcon  : ImageVector
)

private val navItems = listOf(
    NavItem(MainComponent.Config.Dashboard, "Home",      Icons.Filled.Home,      Icons.Outlined.Home),
    NavItem(MainComponent.Config.Analytics, "Analytics", Icons.Filled.Analytics,  Icons.Outlined.Analytics),
    NavItem(MainComponent.Config.Splits,    "Splits",    Icons.Filled.CallSplit,  Icons.Outlined.CallSplit),
    NavItem(MainComponent.Config.Budgets,   "Budgets",   Icons.Filled.Wallet,    Icons.Outlined.Wallet),
    NavItem(MainComponent.Config.Profile,   "Profile",   Icons.Filled.Person,    Icons.Outlined.Person),
)

@Composable
fun LedgerBottomNav(
    currentConfig: MainComponent.Config,
    onTabSelected: (MainComponent.Config) -> Unit
) {
    val colors = LedgerTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceLow)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val isSelected = currentConfig == item.config

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "scale"
            )
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) colors.accentStart else colors.onSurfaceSecondary,
                label = "iconColor"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(item.config) }
                    .padding(vertical = 4.dp)
                    .scale(scale),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = iconColor
                    )
                )
            }
        }
    }
}
