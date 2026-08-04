package com.ryuken.obsidianledger.features.splits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ryuken.obsidianledger.core.domain.model.GroupMember
import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.core.ui.theme.TabularStyle

@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    onAddExpense: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is GroupDetailEffect.Error) snackbarHostState.showSnackbar(effect.message)
        }
    }

    val group = state.group

    Scaffold(
        containerColor = colors.surfaceBase,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (group != null) {
                FloatingActionButton(
                    onClick = { onAddExpense(group.id) },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = colors.accentStart,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurfacePrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = group?.name ?: "Group",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurfacePrimary
                    )
                }
            }

            if (group != null) {
                item {
                    Text(
                        text = "BALANCES",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceSecondary
                    )
                }
                items(state.balances, key = { it.memberId }) { balance ->
                    val member = group.members.firstOrNull { it.id == balance.memberId }
                    if (member != null) {
                        BalanceRow(
                            member  = member,
                            balance = balance,
                            isSelf  = member.userId == state.currentUserId,
                            isSendingRequest = state.sendingRequestForMemberId == member.id,
                            colors  = colors,
                            onEdit   = { viewModel.onIntent(GroupDetailIntent.EditMemberClick(member.id)) },
                            onRemove = { viewModel.onIntent(GroupDetailIntent.RemoveMember(member.id)) },
                            onSettle = { viewModel.onIntent(GroupDetailIntent.SettleUpClick(member.id)) },
                            onSendRequest = { viewModel.onIntent(GroupDetailIntent.SendPaymentRequest(member.id)) }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "EXPENSES",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceSecondary
                    )
                }
                if (state.expenses.isEmpty()) {
                    item {
                        Text(
                            text = "No expenses yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                items(state.expenses, key = { it.id }) { expense ->
                    val payer = group.members.firstOrNull { it.id == expense.paidByMemberId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceLow)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(expense.description, color = colors.onSurfacePrimary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Paid by ${payer?.displayName ?: "someone"}",
                                color = colors.onSurfaceSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "${LedgerTheme.currencySymbol}${expense.amount}",
                            style = TabularStyle(15f, FontWeight.SemiBold).copy(color = colors.onSurfacePrimary)
                        )
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (state.editingMember != null) {
        EditMemberDialog(
            member = state.editingMember!!,
            colors = colors,
            onDismiss = { viewModel.onIntent(GroupDetailIntent.DismissEditMember) },
            onConfirm = { name -> viewModel.onIntent(GroupDetailIntent.ConfirmEditMember(name)) }
        )
    }

    val settlingWith = state.settlingWithMemberId
    if (settlingWith != null && group != null) {
        val counterparty = group.members.firstOrNull { it.id == settlingWith }
        val counterpartyBalance = state.balances.firstOrNull { it.memberId == settlingWith }?.netAmount ?: 0.0
        if (counterparty != null) {
            SettleUpDialog(
                counterpartyName = counterparty.displayName,
                suggestedAmount = kotlin.math.abs(counterpartyBalance),
                defaultIPaid = counterpartyBalance < 0,
                colors = colors,
                onDismiss = { viewModel.onIntent(GroupDetailIntent.DismissSettleUp) },
                onConfirm = { amount, iPaid ->
                    viewModel.onIntent(GroupDetailIntent.ConfirmSettleUp(settlingWith, amount, iPaid))
                }
            )
        }
    }
}

@Composable
private fun BalanceRow(
    member: GroupMember,
    balance: MemberBalance,
    isSelf: Boolean,
    isSendingRequest: Boolean,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onSettle: () -> Unit,
    onSendRequest: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val amountColor = when {
        balance.netAmount > 0.0 -> colors.accentStart
        balance.netAmount < 0.0 -> colors.danger
        else -> colors.onSurfaceSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName + if (isSelf) " (you)" else "",
                color = colors.onSurfacePrimary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = when {
                    balance.netAmount > 0.0 -> "gets back ${LedgerTheme.currencySymbol}${balance.netAmount}"
                    balance.netAmount < 0.0 -> "owes ${LedgerTheme.currencySymbol}${-balance.netAmount}"
                    else -> "settled up"
                },
                color = amountColor,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!isSelf) {
            if (balance.netAmount < 0.0 && member.email != null) {
                IconButton(onClick = onSendRequest, enabled = !isSendingRequest) {
                    Icon(Icons.Default.Email, contentDescription = "Send payment request", tint = colors.onSurfaceSecondary)
                }
            }
            TextButton(onClick = onSettle) {
                Text("Settle", color = colors.accentStart)
            }
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.onSurfaceSecondary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Edit name") }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(text = { Text("Remove") }, onClick = { menuOpen = false; onRemove() })
            }
        }
    }
}

@Composable
private fun EditMemberDialog(
    member: GroupMember,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(member.displayName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceLow,
        title = { Text("Edit member", color = colors.onSurfacePrimary) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("SAVE", color = colors.accentStart)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = colors.onSurfaceSecondary) }
        }
    )
}

@Composable
private fun SettleUpDialog(
    counterpartyName: String,
    suggestedAmount: Double,
    defaultIPaid: Boolean,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf(if (suggestedAmount > 0) suggestedAmount.toString() else "") }
    var iPaid by remember { mutableStateOf(defaultIPaid) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceLow,
        title = { Text("Settle with $counterpartyName", color = colors.onSurfacePrimary) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = iPaid, onClick = { iPaid = true }, label = { Text("I paid") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = !iPaid, onClick = { iPaid = false }, label = { Text("They paid") })
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("Amount") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull() ?: return@TextButton
                if (amount > 0) onConfirm(amount, iPaid)
            }) {
                Text("SETTLE", color = colors.accentStart)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = colors.onSurfaceSecondary) }
        }
    )
}
