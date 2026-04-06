package com.ryuken.obsidianledger.features.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.core.ui.theme.AmountTextStyle
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AddTransactionEffect.SaveSuccess -> onBack()
                is AddTransactionEffect.Error    -> { /* shown in state */ }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .statusBarsPadding()
    ) {
        // ── Top bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.onSurfacePrimary
                )
            }
            Text(
                text = "ADD TRANSACTION",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                color = colors.onSurfaceSecondary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(48.dp))
        }

        // ── Amount Display ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = LedgerTheme.currencySymbol,
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.accentStart,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = state.amount.ifEmpty { "0" },
                    style = AmountTextStyle(40f).copy(
                        color = if (state.amount.isEmpty()) colors.onSurfaceSecondary
                               else colors.accentStart
                    )
                )
            }
        }

        // ── Expense/Income Toggle ───────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TransactionType.entries.forEach { type ->
                val isSelected = type == state.type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) colors.surfaceContainer
                            else Color.Transparent
                        )
                        .clickable { viewModel.onIntent(AddTransactionIntent.TypeChanged(type)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isSelected) colors.accentStart else colors.onSurfaceSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Category Chips ──────────────────────────────────────
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.categories, key = { it.id }) { category ->
                CategoryChip(
                    category = category,
                    isSelected = category == state.selectedCategory,
                    accentColor = colors.accentStart,
                    surfaceColor = colors.surfaceLow,
                    textColor = colors.onSurfacePrimary,
                    onClick = { viewModel.onIntent(AddTransactionIntent.CategorySelected(category)) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Note Field ──────────────────────────────────────────
        TextField(
            value = state.note,
            onValueChange = { viewModel.onIntent(AddTransactionIntent.NoteChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = {
                Text("Add a note...", color = colors.onSurfaceSecondary)
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfacePrimary),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = colors.accentStart,
                unfocusedIndicatorColor = colors.ghostBorder,
                cursorColor             = colors.accentStart
            )
        )

        // ── Date Row ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = "Date",
                tint = colors.onSurfaceSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.date.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfacePrimary
            )
        }



        // ── Numpad ──────────────────────────────────────────────────
        NumpadGrid(
            onKey = { viewModel.onIntent(AddTransactionIntent.NumpadInput(it)) },
            onDelete = { viewModel.onIntent(AddTransactionIntent.NumpadDelete) },
            colors = colors
        )

        Spacer(Modifier.height(12.dp))

        // ── Save Button ─────────────────────────────────────────
        Button(
            onClick = { viewModel.onIntent(AddTransactionIntent.SaveClick) },
            enabled = state.canSave && !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accentStart,
                contentColor = Color.White,
                disabledContainerColor = colors.surfaceContainer
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "SAVE TRANSACTION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }


    }
}

// ═══════════════════════════════════════════════════════════════════════
// Category Chip
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColor)
            .then(
                if (isSelected) Modifier.border(1.5.dp, accentColor, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = category.emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isSelected) accentColor else textColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Custom Numpad (3x4 grid)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun NumpadGrid(
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceLow)
                            .clickable {
                                if (key == "⌫") onDelete() else onKey(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = colors.onSurfacePrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
