package com.ryuken.obsidianledger.core.ui.animation

import androidx.compose.animation.core.*

object LedgerAnimations {

    // Standard spring for most UI elements
    val springStandard = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    // Gentle spring for large elements like cards
    val springGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness    = Spring.StiffnessLow
    )

    // No bounce for functional elements like progress bars
    val springNoBounce = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    // Standard tween for opacity changes
    val tweenFade = tween<Float>(
        durationMillis = 250,
        easing         = FastOutSlowInEasing
    )

    // Fast tween for quick feedback
    val tweenFast = tween<Float>(
        durationMillis = 150,
        easing         = FastOutLinearInEasing
    )

    // Slow tween for entering elements
    val tweenEnter = tween<Float>(
        durationMillis = 400,
        easing         = FastOutSlowInEasing
    )

    // Stagger delay per list item
    fun staggerDelay(index: Int, baseDelay: Int = 40): Int =
        (index * baseDelay).coerceAtMost(400)
}
