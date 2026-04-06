package com.ryuken.obsidianledger.features.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.BudgetStatus
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.core.ui.theme.TabularStyle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceBase)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──────────────────────────────────────────
            item {
                Text(
                    text = "PORTFOLIO CONTROL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 3.sp,
                        color = colors.accentStart
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Budgets",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurfacePrimary
                )
            }

            if (state.budgets.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No budgets yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Set spending limits to control your finances",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceSecondary
                            )
                        }
                    }
                }
            }

            // ── Budget Cards ────────────────────────────────────
            items(state.budgets, key = { it.id }) { budget ->
                BudgetCard(
                    budget = budget,
                    colors = colors,
                    onDelete = { viewModel.onIntent(BudgetsIntent.DeleteBudget(budget.id)) }
                )
            }

            // Bottom spacer for FAB
            item { Spacer(Modifier.height(72.dp)) }
        }

        // ── FAB ─────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { viewModel.onIntent(BudgetsIntent.AddBudgetClick) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.accentStart,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Budget")
        }
    }

    // ── Add Budget Dialog ────────────────────────────────────────
    if (state.showAddDialog) {
        AddBudgetDialog(
            categories = state.categories,
            colors = colors,
            onDismiss = { viewModel.onIntent(BudgetsIntent.DismissDialog) },
            onConfirm = { cat, limit ->
                viewModel.onIntent(BudgetsIntent.ConfirmAddBudget(cat, limit))
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Budget Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun BudgetCard(
    budget: Budget,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    onDelete: () -> Unit
) {
    val progressColor = when (budget.status) {
        BudgetStatus.HEALTHY    -> colors.accentStart
        BudgetStatus.WARNING    -> colors.warning
        BudgetStatus.HIGH_ALERT,
        BudgetStatus.CRITICAL,
        BudgetStatus.EXCEEDED   -> colors.danger
    }

    val hasDangerBorder = budget.status.isAlert

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
            .then(
                if (hasDangerBorder) Modifier.drawBehind {
                    drawLine(
                        color       = colors.danger,
                        start       = Offset(0f, 0f),
                        end         = Offset(0f, size.height),
                        strokeWidth = 4.dp.toPx()
                    )
                } else Modifier
            )
            .padding(16.dp)
    ) {
        // Emoji avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = budget.category.emoji, fontSize = 20.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = budget.category.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.onSurfacePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Status badge for CRITICAL / EXCEEDED
                if (budget.status == BudgetStatus.CRITICAL || budget.status == BudgetStatus.EXCEEDED) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.dangerContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = budget.status.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = colors.danger,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Spent vs limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${LedgerTheme.currencySymbol}${formatAmt(budget.spent)} / ${LedgerTheme.currencySymbol}${formatAmt(budget.limitAmount)}",
                    style = TabularStyle(13f).copy(color = colors.onSurfaceSecondary)
                )
                Text(
                    text = "${budget.percentUsed.toInt()}%",
                    style = TabularStyle(13f, FontWeight.SemiBold).copy(color = progressColor)
                )
            }

            Spacer(Modifier.height(6.dp))

            val animatedProgress by animateFloatAsState(
                targetValue   = (budget.percentUsed / 100.0).toFloat().coerceIn(0f, 1f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness    = Spring.StiffnessLow
                ),
                label = "budgetProgress"
            )

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = colors.surfaceContainer
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${budget.daysRemainingInPeriod} days left",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceSecondary
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = colors.onSurfaceSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Add Budget Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun AddBudgetDialog(
    categories: List<Category>,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    onDismiss: () -> Unit,
    onConfirm: (Category, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var limitText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceLow,
        title = {
            Text(
                text = "Add Budget",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurfacePrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceSecondary
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (cat == selectedCategory) colors.surfaceContainer
                                    else colors.surfaceHigh
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${cat.emoji} ${cat.name}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (cat == selectedCategory) colors.accentStart
                                           else colors.onSurfacePrimary
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "MONTHLY LIMIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceSecondary
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurfacePrimary),
                    placeholder = { Text("${LedgerTheme.currencySymbol} Amount", color = colors.onSurfaceSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = colors.accentStart,
                        unfocusedIndicatorColor = colors.ghostBorder,
                        cursorColor             = colors.accentStart
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cat = selectedCategory ?: return@TextButton
                    val limit = limitText.toDoubleOrNull() ?: return@TextButton
                    if (limit > 0) onConfirm(cat, limit)
                },
                enabled = selectedCategory != null && (limitText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("ADD", color = colors.accentStart)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.onSurfaceSecondary)
            }
        }
    )
}

private fun formatAmt(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        val rounded = kotlin.math.round(amount * 100) / 100.0
        val str = rounded.toString()
        val parts = str.split(".")
        if (parts.size == 2 && parts[1].length == 1) "${parts[0]}.${parts[1]}0" else str
    }
}
