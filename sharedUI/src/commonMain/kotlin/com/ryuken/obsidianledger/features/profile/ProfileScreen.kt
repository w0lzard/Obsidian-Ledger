package com.ryuken.obsidianledger.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.core.ui.theme.SpaceGroteskFamily
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProfileEffect.SignedOut        -> onSignedOut()
                is ProfileEffect.CsvExported   -> { /* TODO: share intent */ }
                is ProfileEffect.Error         -> { /* shown as snackbar */ }
                ProfileEffect.SyncComplete     -> { }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Avatar ──────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.initials,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = SpaceGroteskFamily(),
                        fontWeight = FontWeight.Bold,
                        color = colors.accentStart
                    )
                )
            }
        }

        // ── Name / Email ────────────────────────────────────────
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.profile?.displayName ?: "Loading...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurfacePrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.profile?.email ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceSecondary
                )
            }
        }

        // ── Stat Pills ──────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill("Transactions", "${state.transactionCount}", colors, Modifier.weight(1f))
                StatPill("Budgets", "${state.activeBudgets}", colors, Modifier.weight(1f))
                StatPill("Member Since", state.memberSince, colors, Modifier.weight(1f))
            }
        }

        // ── Preferences Section ─────────────────────────────────
        item {
            SettingsSection(title = "PREFERENCES") {
                SettingsRow(icon = Icons.Default.CurrencyRupee, label = "Currency", value = "INR (₹)", colors = colors)
                SettingsRow(icon = Icons.Default.DarkMode, label = "Theme", value = "System", colors = colors)
                SettingsRow(icon = Icons.Default.Notifications, label = "Notifications", value = "On", colors = colors)
            }
        }

        // ── Data Section ────────────────────────────────────────
        item {
            SettingsSection(title = "DATA") {
                SettingsRow(
                    icon = Icons.Default.FileDownload,
                    label = "Export CSV",
                    colors = colors,
                    onClick = { viewModel.onIntent(ProfileIntent.ExportCsv) }
                )
                SettingsRow(icon = Icons.Default.FileUpload, label = "Import", colors = colors)
                SettingsRow(
                    icon = Icons.Default.Sync,
                    label = "Sync Status",
                    value = state.lastSyncTimestamp,
                    colors = colors,
                    onClick = { viewModel.onIntent(ProfileIntent.SyncNow) }
                )
            }
        }

        // ── Account Section ─────────────────────────────────────
        item {
            SettingsSection(title = "ACCOUNT") {
                SettingsRow(icon = Icons.Default.Edit, label = "Edit Profile", colors = colors)
                SettingsRow(icon = Icons.Default.Lock, label = "Change Password", colors = colors)
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Log Out",
                    colors = colors,
                    isDanger = true,
                    onClick = { viewModel.onIntent(ProfileIntent.SignOut) }
                )
            }
        }

        // ── Version ─────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Obsidian Ledger v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Stat Pill
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun StatPill(
    label: String,
    value: String,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurfacePrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = colors.onSurfaceSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Settings Section
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = LedgerTheme.colors
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.sp,
                color = colors.onSurfaceSecondary
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceLow)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    isDanger: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDanger) colors.danger else colors.onSurfaceSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isDanger) colors.danger else colors.onSurfacePrimary,
                fontWeight = if (isDanger) FontWeight.SemiBold else FontWeight.Normal
            ),
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceSecondary
            )
        }
    }
}
