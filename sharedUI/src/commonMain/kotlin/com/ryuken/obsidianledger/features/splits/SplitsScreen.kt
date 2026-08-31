package com.ryuken.obsidianledger.features.splits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.domain.model.SplitGroup
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplitsScreen(
    onNavigateToGroup: (String) -> Unit,
    onCreateGroup: () -> Unit,
    viewModel: SplitsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onIntent(SplitsIntent.Refresh) }

    // Channel-backed one-shot effects: safe under recomposition and config changes.
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SplitsEffect.GroupCreated -> onNavigateToGroup(effect.groupId)
                is SplitsEffect.Error        -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        containerColor = colors.surfaceBase,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceBase).padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "SHARED EXPENSES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 3.sp,
                        color = colors.accentStart
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Splits",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurfacePrimary
                )
            }

            if (state.groups.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No groups yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Create a group to start splitting expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceSecondary
                            )
                        }
                    }
                }
            }

            items(state.groups, key = { it.id }) { group ->
                GroupCard(
                    group = group,
                    colors = colors,
                    onClick = { onNavigateToGroup(group.id) }
                )
            }

            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = onCreateGroup,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.accentStart,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Group")
        }
    }
    }
}

@Composable
private fun GroupCard(
    group: SplitGroup,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = colors.accentStart)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.onSurfacePrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${group.members.size} member${if (group.members.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceSecondary
            )
        }
    }
}
