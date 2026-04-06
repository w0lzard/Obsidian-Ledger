package com.ryuken.obsidianledger.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.ryuken.obsidianledger.core.ui.animation.LedgerAnimations

// Animated scale on press — wrap any clickable composable
@Composable
fun Modifier.pressScale(
    pressedScale : Float = 0.96f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue    = if (isPressed) pressedScale else 1f,
        animationSpec  = LedgerAnimations.springStandard,
        label          = "pressScale"
    )
    return this.scale(scale)
}

// Fade + slide in for list items with stagger
@Composable
fun AnimatedListItem(
    index   : Int,
    visible : Boolean = true,
    content : @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(LedgerAnimations.staggerDelay(index).toLong())
            isVisible = true
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter   = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis    = 0,
                easing         = FastOutSlowInEasing
            )
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness    = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { it / 4 }
        )
    ) {
        content()
    }
}

// Number counter animation — smooth roll up/down for amounts
@Composable
fun animateDoubleAsState(
    targetValue   : Double,
    animationSpec : AnimationSpec<Float> = LedgerAnimations.springNoBounce
): Double {
    val animatedValue by animateFloatAsState(
        targetValue   = targetValue.toFloat(),
        animationSpec = animationSpec,
        label         = "animatedDouble"
    )
    return animatedValue.toDouble()
}

// Shimmer loading effect for skeleton screens
@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue   = 0.3f,
        targetValue    = 0.7f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(
                durationMillis = 800,
                easing         = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    return this.graphicsLayer { this.alpha = alpha }
}
