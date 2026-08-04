package com.ryuken.obsidianledger.features.splits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme

@Composable
fun AddSplitExpenseScreen(
    viewModel: AddSplitExpenseViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AddSplitExpenseEffect.SaveSuccess -> onBack()
                is AddSplitExpenseEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val group = state.group

    Scaffold(
        containerColor = colors.surfaceBase,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
                Text("Add Expense", style = MaterialTheme.typography.headlineSmall, color = colors.onSurfacePrimary)
            }

            Spacer(Modifier.height(20.dp))

            TextField(
                value = state.description,
                onValueChange = { viewModel.onIntent(AddSplitExpenseIntent.DescriptionChanged(it)) },
                placeholder = { Text("Description", color = colors.onSurfaceSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            TextField(
                value = state.amount,
                onValueChange = { viewModel.onIntent(AddSplitExpenseIntent.AmountChanged(it)) },
                placeholder = { Text("${LedgerTheme.currencySymbol} Amount", color = colors.onSurfaceSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (group != null) {
                Spacer(Modifier.height(20.dp))
                Text("PAID BY", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp), color = colors.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(group.members, key = { it.id }) { member ->
                        val selected = state.payerMemberId == member.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) colors.surfaceContainer else colors.surfaceHigh)
                                .clickable { viewModel.onIntent(AddSplitExpenseIntent.PayerSelected(member.id)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                member.displayName,
                                color = if (selected) colors.accentStart else colors.onSurfacePrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("SPLIT BETWEEN", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp), color = colors.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(group.members, key = { it.id }) { member ->
                        val checked = member.id in state.participantMemberIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onIntent(AddSplitExpenseIntent.ToggleParticipant(member.id)) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { viewModel.onIntent(AddSplitExpenseIntent.ToggleParticipant(member.id)) })
                            Spacer(Modifier.width(8.dp))
                            Text(member.displayName, color = colors.onSurfacePrimary)
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            Button(
                onClick = { viewModel.onIntent(AddSplitExpenseIntent.SaveClick) },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentStart, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isSaving) "Saving…" else "Save Expense")
            }
        }
    }
}
