package com.ryuken.obsidianledger.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetStatus
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.ui.theme.AmountTextStyle
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.core.ui.theme.TabularStyle
import com.ryuken.obsidianledger.core.ui.components.AnimatedListItem
import com.ryuken.obsidianledger.core.ui.components.animateDoubleAsState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceBase)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Greeting ────────────────────────────────────────
            item {
                Column {
                    Text(
                        text = "${greeting()},",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceSecondary
                    )
                    Text(
                        text = state.userName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurfacePrimary
                    )
                }
            }

            // ── Hero Balance ────────────────────────────────────
            item {
                HeroBalanceCard(
                    balance = state.balance,
                    income = state.summary.totalIncome,
                    expense = state.summary.totalExpense,
                    colors = colors
                )
            }

            // ── Budget Preview Strip ────────────────────────────
            if (state.budgets.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "BUDGET OVERVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.budgets, key = { it.id }) { budget ->
                                BudgetPreviewChip(budget = budget, colors = colors)
                            }
                        }
                    }
                }
            }

            // ── Splits Summary ───────────────────────────────────
            item {
                SplitsSummaryCard(activeGroups = state.activeSplitGroups, colors = colors)
            }

            // ── Recent Transactions ─────────────────────────────
            item {
                Text(
                    text = "RECENT TRANSACTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceSecondary
                )
            }

            if (state.recentTransactions.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceSecondary
                        )
                    }
                }
            }

            items(state.recentTransactions, key = { it.id }) { transaction ->
                val index = state.recentTransactions.indexOf(transaction)
                AnimatedListItem(index = index) {
                    TransactionItem(transaction = transaction, colors = colors)
                }
            }
        }

        // ── FAB ─────────────────────────────────────────────────
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.accentStart,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Hero Balance Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun HeroBalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
            .padding(24.dp)
    ) {
        Text(
            text = "CURRENT BALANCE",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceSecondary
        )
        Spacer(Modifier.height(8.dp))
        val animatedBalance = animateDoubleAsState(targetValue = balance)

        Text(
            text = "${LedgerTheme.currencySymbol}${formatAmount(animatedBalance)}",
            style = AmountTextStyle(40f).copy(
                color = colors.accentStart
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Net savings this month",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceSecondary
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IncomeExpenseLabel(
                label = "Income",
                amount = income,
                color = colors.accentStart,
                colors = colors
            )
            IncomeExpenseLabel(
                label = "Expense",
                amount = expense,
                color = colors.danger,
                colors = colors
            )
        }
    }
}

@Composable
private fun IncomeExpenseLabel(
    label: String,
    amount: Double,
    color: Color,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceSecondary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${LedgerTheme.currencySymbol}${formatAmount(amount)}",
            style = TabularStyle(18f, FontWeight.SemiBold).copy(
                color = colors.onSurfacePrimary
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Budget Preview Chip
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun BudgetPreviewChip(
    budget: Budget,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors
) {
    val progressColor = when (budget.status) {
        BudgetStatus.HEALTHY    -> colors.accentStart
        BudgetStatus.WARNING    -> colors.warning
        BudgetStatus.HIGH_ALERT,
        BudgetStatus.CRITICAL,
        BudgetStatus.EXCEEDED   -> colors.danger
    }

    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
            .padding(12.dp)
    ) {
        Text(
            text = "${budget.category.emoji} ${budget.category.name}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfacePrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (budget.percentUsed / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = progressColor,
            trackColor = colors.surfaceContainer
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${budget.percentUsed.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = progressColor
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Transaction Item
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun TransactionItem(
    transaction: Transaction,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category emoji avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.category.emoji,
                fontSize = 20.sp
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfacePrimary,
                fontWeight = FontWeight.Medium
            )
            if (!transaction.note.isNullOrBlank()) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${LedgerTheme.currencySymbol}${formatAmount(transaction.amount)}",
            style = TabularStyle(16f, FontWeight.SemiBold).copy(
                color = if (transaction.type == TransactionType.EXPENSE) colors.danger else colors.accentStart
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════════════

private fun formatAmount(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    return if (abs == abs.toLong().toDouble()) {
        abs.toLong().toString()
    } else {
        val rounded = kotlin.math.round(abs * 100) / 100.0
        val str = rounded.toString()
        val parts = str.split(".")
        if (parts.size == 2 && parts[1].length == 1) "${parts[0]}.${parts[1]}0" else str
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Splits Summary Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SplitsSummaryCard(
    activeGroups: Int,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors
) {
    if (activeGroups == 0) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
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
            Text(text = "👥", fontSize = 20.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = "Active Split Groups",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfacePrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "You are part of $activeGroups ${if (activeGroups == 1) "group" else "groups"}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceSecondary
            )
        }
    }
}
