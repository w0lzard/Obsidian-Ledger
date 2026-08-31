package com.ryuken.obsidianledger.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import com.ryuken.obsidianledger.core.ui.theme.SpaceGroteskFamily

// Cold-start auth resolution + first data fetch both take a moment — this gives them
// room to finish behind an intentional animation instead of a flash of the wrong screen.
const val SPLASH_MIN_DURATION_MS = 2000L

// Hard ceiling on how long the splash may wait for the auth state to leave Initializing.
// Session restore reads local encrypted storage first, so a signed-in user resolves in
// milliseconds; only a wedged session/refresh hits this and falls back to sign-in.
const val AUTH_RESOLVE_TIMEOUT_MS = 10_000L

@Composable
fun SplashScreen(
    durationMillis: Long = SPLASH_MIN_DURATION_MS,
    modifier: Modifier = Modifier
) {
    val colors = LedgerTheme.colors

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val entrance by animateFloatAsState(
        targetValue   = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label         = "splashEntrance"
    )
    val progress by animateFloatAsState(
        targetValue   = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = (durationMillis - 300L).coerceAtLeast(200L).toInt(), easing = LinearEasing),
        label         = "splashProgress"
    )
    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val pulseScale by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.035f,
        animationSpec = infiniteRepeatable(
            animation   = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode  = RepeatMode.Reverse
        ),
        label = "splashPulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Obsidian Ledger",
                style = TextStyle(
                    fontFamily = SpaceGroteskFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 28.sp,
                    brush      = Brush.linearGradient(listOf(colors.accentStart, colors.accentEnd))
                ),
                modifier = Modifier.graphicsLayer {
                    val scale = (0.85f + entrance * 0.15f) * pulseScale
                    scaleX = scale
                    scaleY = scale
                    alpha  = entrance
                }
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surfaceHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(colors.accentStart, colors.accentEnd)))
                )
            }
        }
    }
}
