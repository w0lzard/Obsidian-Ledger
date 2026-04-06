package com.ryuken.obsidianledger.features.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.ui.theme.AmountTextStyle
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.core.ui.theme.TabularStyle
import kotlinx.datetime.Month
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ──────────────────────────────────────────────
        item {
            Text(
                text = "ANALYTICS",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 3.sp,
                    color = colors.accentStart
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Spending Insights",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onSurfacePrimary
            )
        }

        // ── Month Selector ──────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${Month(state.selectedMonth).name.take(3)} ${state.selectedYear}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfacePrimary
                )
            }
        }

        // ── Total Outflow Card ──────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceLow)
                    .padding(24.dp)
            ) {
                Text(
                    text = "TOTAL OUTFLOW",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceSecondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${LedgerTheme.currencySymbol}${formatAmount(state.totalOutflow)}",
                    style = TabularStyle(28f, FontWeight.Bold).copy(color = LedgerTheme.colors.onSurfacePrimary)
                )
                Spacer(Modifier.height(4.dp))
                val delta = state.monthOverMonthDelta
                val deltaColor = if (delta <= 0) colors.accentStart else colors.danger
                val deltaPrefix = if (delta > 0) "+" else ""
                Text(
                    text = "${deltaPrefix}${(kotlin.math.round(delta * 10) / 10.0).toString()}% vs last month",
                    style = MaterialTheme.typography.bodySmall.copy(color = deltaColor)
                )
            }
        }

        // ── Sparkline ───────────────────────────────────────────
        if (state.sparklineData.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceLow)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "6-MONTH TREND",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    SparklineChart(
                        data = state.sparklineData,
                        color = colors.accentStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                }
            }
        }

        // ── Category Breakdown ──────────────────────────────────
        if (state.categoryBreakdown.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceLow)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "CATEGORY BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    val maxAmount = state.categoryBreakdown.values.maxOrNull() ?: 1.0
                    state.categoryBreakdown.entries
                        .sortedByDescending { it.value }
                        .forEach { (categoryId, amount) ->
                            CategoryBar(
                                name = categoryId,
                                amount = amount,
                                fraction = (amount / maxAmount).toFloat(),
                                colors = colors
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                }
            }
        }

        // ── Stat Mini-Cards ─────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "SAVINGS RATE",
                    value = "${(kotlin.math.round(state.savingsRate * 10) / 10.0).toString()}%",
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "AVG TRANSACTION",
                    value = "${LedgerTheme.currencySymbol}${formatAmount(state.avgTransaction)}",
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── AI Insight Card ─────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceLow)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(colors.accentStart)
                )
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "INSIGHT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentStart,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.savingsRate > 20)
                            "Great job! You're saving ${kotlin.math.round(state.savingsRate).toLong().toString()}% of your income this month. Keep up the momentum."
                        else if (state.totalOutflow > 0)
                            "Your spending has ${if (state.monthOverMonthDelta > 0) "increased" else "decreased"} compared to last month. Consider reviewing your top expense categories."
                        else
                            "Start tracking your expenses to get personalized insights.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfacePrimary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Sparkline Chart (Canvas-based)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SparklineChart(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    Box(
        modifier = modifier.drawBehind {
            val maxVal = data.max().coerceAtLeast(1.0)
            val minVal = data.min()
            val range = (maxVal - minVal).coerceAtLeast(1.0)
            val step = size.width / (data.size - 1)

            for (i in 0 until data.size - 1) {
                val x0 = i * step
                val y0 = size.height - ((data[i] - minVal) / range * size.height).toFloat()
                val x1 = (i + 1) * step
                val y1 = size.height - ((data[i + 1] - minVal) / range * size.height).toFloat()

                drawLine(
                    color       = color,
                    start       = Offset(x0, y0),
                    end         = Offset(x1, y1),
                    strokeWidth = 3f,
                    cap         = StrokeCap.Round
                )
            }

            // Draw dots
            data.forEachIndexed { i, value ->
                val x = i * step
                val y = size.height - ((value - minVal) / range * size.height).toFloat()
                drawCircle(color = color, radius = 4f, center = Offset(x, y))
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Category Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CategoryBar(
    name: String,
    amount: Double,
    fraction: Float,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfacePrimary
            )
            Text(
                text = "${LedgerTheme.currencySymbol}${formatAmount(amount)}",
                style = TabularStyle(14f, FontWeight.Medium).copy(color = LedgerTheme.colors.onSurfacePrimary)
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = colors.accentStart,
            trackColor = colors.surfaceContainer
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Stat Card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun StatCard(
    label: String,
    value: String,
    colors: com.ryuken.obsidianledger.core.ui.theme.LedgerColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceLow)
            .padding(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceSecondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = TabularStyle(20f, FontWeight.SemiBold).copy(
                color = colors.onSurfacePrimary
            )
        )
    }
}

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
