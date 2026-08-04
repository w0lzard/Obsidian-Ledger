package com.ryuken.obsidianledger.features.splits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: SplitsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors

    var groupName by remember { mutableStateOf("") }
    var memberNames by remember { mutableStateOf(listOf("")) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SplitsEffect.GroupCreated -> onCreated()
                is SplitsEffect.Error -> { /* surfaced via snackbar below */ }
            }
        }
    }

    Scaffold(containerColor = colors.surfaceBase) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurfacePrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "New Group",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurfacePrimary
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "GROUP NAME",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = colors.onSurfaceSecondary
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = groupName,
                onValueChange = { groupName = it },
                placeholder = { Text("e.g. Goa Trip", color = colors.onSurfaceSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = colors.surfaceLow,
                    unfocusedContainerColor = colors.surfaceLow,
                    focusedIndicatorColor   = colors.accentStart,
                    unfocusedIndicatorColor = colors.ghostBorder,
                    cursorColor             = colors.accentStart,
                    focusedTextColor        = colors.onSurfacePrimary,
                    unfocusedTextColor      = colors.onSurfacePrimary
                )
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "MEMBERS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = colors.onSurfaceSecondary
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(memberNames.size) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = memberNames[index],
                            onValueChange = { value ->
                                memberNames = memberNames.toMutableList().also { it[index] = value }
                            },
                            placeholder = { Text("Member name", color = colors.onSurfaceSecondary) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = colors.surfaceLow,
                                unfocusedContainerColor = colors.surfaceLow,
                                focusedIndicatorColor   = colors.accentStart,
                                unfocusedIndicatorColor = colors.ghostBorder,
                                cursorColor             = colors.accentStart,
                                focusedTextColor        = colors.onSurfacePrimary,
                                unfocusedTextColor      = colors.onSurfacePrimary
                            )
                        )
                        if (memberNames.size > 1) {
                            IconButton(onClick = {
                                memberNames = memberNames.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = colors.onSurfaceSecondary)
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { memberNames = memberNames + "" }) {
                        Text("+ Add member", color = colors.accentStart)
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.onIntent(
                        SplitsIntent.CreateGroup(
                            name = groupName.trim(),
                            memberNames = memberNames.map { it.trim() }.filter { it.isNotBlank() }
                        )
                    )
                },
                enabled = groupName.isNotBlank() && !state.isCreatingGroup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentStart,
                    contentColor   = Color.White
                )
            ) {
                Text(if (state.isCreatingGroup) "Creating…" else "Create Group")
            }
        }
    }
}
