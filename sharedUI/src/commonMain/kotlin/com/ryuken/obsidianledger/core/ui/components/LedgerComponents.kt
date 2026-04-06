package com.ryuken.obsidianledger.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme

@Composable
fun LedgerPrimaryButton(
    text      : String,
    onClick   : () -> Unit,
    isLoading : Boolean  = false,
    enabled   : Boolean  = true,
    modifier  : Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    Button(
        onClick           = onClick,
        enabled           = enabled && !isLoading,
        interactionSource = interactionSource,
        modifier          = modifier
            .fillMaxWidth()
            .scale(scale),
        colors            = ButtonDefaults.buttonColors(
            containerColor        = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape             = RoundedCornerShape(8.dp)
    ) {
        AnimatedContent(
            targetState   = isLoading,
            transitionSpec = {
                fadeIn(tween(150)) togetherWith fadeOut(tween(150))
            },
            label = "buttonContent"
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color    = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text  = text,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun LedgerCard(
    modifier    : Modifier = Modifier,
    borderStart : Color    = Color.Transparent,
    content     : @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                  slideInVertically(
                      spring(
                          dampingRatio = Spring.DampingRatioNoBouncy,
                          stiffness    = Spring.StiffnessMediumLow
                      )
                  ) { it / 6 }
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(
                containerColor = LedgerTheme.colors.surfaceContainer
            ),
            shape    = RoundedCornerShape(12.dp)
        ) {
            if (borderStart != Color.Transparent) {
                Row {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(borderStart)
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        content  = content
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(16.dp),
                    content  = content
                )
            }
        }
    }
}
