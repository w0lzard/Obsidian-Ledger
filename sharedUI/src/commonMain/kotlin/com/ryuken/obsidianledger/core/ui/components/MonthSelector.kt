package com.ryuken.obsidianledger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import kotlinx.datetime.Month

/**
 * ‹ MON YYYY › month navigation strip shared by Dashboard and Analytics. Purely
 * presentational — month math happens in the caller and arrives as callbacks, so
 * the MVI intent flow stays the single source of state changes.
 */
@Composable
fun MonthSelector(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = "Previous month",
            tint = LedgerTheme.colors.onSurfaceSecondary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onPrevious)
                .padding(8.dp)
        )
        Text(
            text = "${Month(month).name.take(3)} $year",
            style = MaterialTheme.typography.titleMedium,
            color = LedgerTheme.colors.onSurfacePrimary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Next month",
            tint = LedgerTheme.colors.onSurfaceSecondary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onNext)
                .padding(8.dp)
        )
    }
}
