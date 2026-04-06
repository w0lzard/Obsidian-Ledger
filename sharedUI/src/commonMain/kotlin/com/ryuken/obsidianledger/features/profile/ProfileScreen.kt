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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProfileEffect.SignedOut        -> onSignedOut()
                is ProfileEffect.CsvExported   -> snackbarHostState.showSnackbar("CSV exported")
                is ProfileEffect.Error         -> snackbarHostState.showSnackbar(effect.message)
                ProfileEffect.SyncComplete     -> snackbarHostState.showSnackbar("Sync complete")
                is ProfileEffect.ShowMessage   -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    if (state.isCurrencyDialogOpen) {
        CurrencyDialog(
            currentValue = state.currency,
            onDismiss = { viewModel.onIntent(ProfileIntent.ToggleCurrencyDialog(false)) },
            onSelect = { viewModel.onIntent(ProfileIntent.SetCurrency(it)) }
        )
    }

    if (state.isThemeDialogOpen) {
        ThemeDialog(
            currentValue = state.theme,
            onDismiss = { viewModel.onIntent(ProfileIntent.ToggleThemeDialog(false)) },
            onSelect = { viewModel.onIntent(ProfileIntent.SetTheme(it)) }
        )
    }

    if (state.isImportDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ProfileIntent.ToggleImportDialog(false)) },
            title = { Text("Import Data", color = colors.onSurfacePrimary) },
            text = { Text("Direct CSV imports are currently only supported via the web dashboard. Local native file picking will be available soon.", color = colors.onSurfaceSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(ProfileIntent.ToggleImportDialog(false)) }) {
                    Text("Got it", color = colors.accentStart)
                }
            },
            containerColor = colors.surfaceContainer,
            titleContentColor = colors.onSurfacePrimary
        )
    }

    if (state.isEditProfileDialogOpen) {
        var newName by remember { mutableStateOf(state.profile?.displayName ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ProfileIntent.ToggleEditProfileDialog(false)) },
            title = { Text("Edit Profile", color = colors.onSurfacePrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentStart,
                        unfocusedBorderColor = colors.ghostBorder,
                        focusedTextColor = colors.onSurfacePrimary,
                        unfocusedTextColor = colors.onSurfacePrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(ProfileIntent.UpdateDisplayName(newName)) }, enabled = !state.isAuthActionLoading) {
                    Text(if (state.isAuthActionLoading) "Saving..." else "Save", color = colors.accentStart)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ProfileIntent.ToggleEditProfileDialog(false)) }, enabled = !state.isAuthActionLoading) {
                    Text("Cancel", color = colors.onSurfaceSecondary)
                }
            },
            containerColor = colors.surfaceContainer,
            titleContentColor = colors.onSurfacePrimary
        )
    }

    if (state.isChangePasswordDialogOpen) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ProfileIntent.ToggleChangePasswordDialog(false)) },
            title = { Text("Change Password", color = colors.onSurfacePrimary) },
            text = {
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentStart,
                        unfocusedBorderColor = colors.ghostBorder,
                        focusedTextColor = colors.onSurfacePrimary,
                        unfocusedTextColor = colors.onSurfacePrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(ProfileIntent.UpdatePassword(newPass)) }, enabled = !state.isAuthActionLoading) {
                    Text(if (state.isAuthActionLoading) "Updating..." else "Update", color = colors.accentStart)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ProfileIntent.ToggleChangePasswordDialog(false)) }, enabled = !state.isAuthActionLoading) {
                    Text("Cancel", color = colors.onSurfaceSecondary)
                }
            },
            containerColor = colors.surfaceContainer,
            titleContentColor = colors.onSurfacePrimary
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.surfaceBase
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text  = "Loading profile...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (state.error != null && state.profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text  = state.error ?: "Something went wrong",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { viewModel.retry() },
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("RETRY")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                            text = state.profile?.displayName ?: "—",
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

                item {
                    SettingsSection(title = "PREFERENCES") {
                        SettingsRow(
                            icon = Icons.Default.CurrencyRupee, 
                            label = "Currency", 
                            value = state.currency, 
                            colors = colors,
                            onClick = { viewModel.onIntent(ProfileIntent.ToggleCurrencyDialog(true)) }
                        )
                        SettingsRow(
                            icon = Icons.Default.DarkMode, 
                            label = "Theme", 
                            value = state.theme, 
                            colors = colors,
                            onClick = { viewModel.onIntent(ProfileIntent.ToggleThemeDialog(true)) }
                        )
                        SettingsRow(
                            icon = Icons.Default.Notifications, 
                            label = "Notifications", 
                            value = if (state.notificationsEnabled) "On" else "Off", 
                            colors = colors,
                            onClick = { viewModel.onIntent(ProfileIntent.ToggleNotifications(!state.notificationsEnabled)) }
                        )
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
                        SettingsRow(
                            icon = Icons.Default.FileUpload, 
                            label = "Import", 
                            colors = colors,
                            onClick = { viewModel.onIntent(ProfileIntent.ToggleImportDialog(true)) }
                        )
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
                        SettingsRow(
                            icon = Icons.Default.Edit, 
                            label = "Edit Profile", 
                            colors = colors,
                            onClick = { viewModel.onIntent(ProfileIntent.ToggleEditProfileDialog(true)) }
                        )
                        SettingsRow(
                            icon = Icons.Default.Lock, 
                            label = "Change Password", 
                            colors = colors,
                            onClick = { viewModel.onIntent(ProfileIntent.ToggleChangePasswordDialog(true)) }
                        )
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

// ═══════════════════════════════════════════════════════════════════════
// Helper Dialogs
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CurrencyDialog(currentValue: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val colors = LedgerTheme.colors
    val currencies = listOf("INR (₹)", "USD ($)", "EUR (€)", "GBP (£)")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency", color = colors.onSurfacePrimary) },
        text = {
            Column {
                currencies.forEach { curr ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(curr) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (curr == currentValue),
                            onClick = { onSelect(curr) },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accentStart, unselectedColor = colors.onSurfaceSecondary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(curr, color = colors.onSurfacePrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceSecondary) }
        },
        containerColor = colors.surfaceContainer,
        titleContentColor = colors.onSurfacePrimary
    )
}

@Composable
private fun ThemeDialog(currentValue: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val colors = LedgerTheme.colors
    val themes = listOf("System", "Light", "Dark")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme", color = colors.onSurfacePrimary) },
        text = {
            Column {
                themes.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(t) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (t == currentValue),
                            onClick = { onSelect(t) },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accentStart, unselectedColor = colors.onSurfaceSecondary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(t, color = colors.onSurfacePrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceSecondary) }
        },
        containerColor = colors.surfaceContainer,
        titleContentColor = colors.onSurfacePrimary
    )
}
