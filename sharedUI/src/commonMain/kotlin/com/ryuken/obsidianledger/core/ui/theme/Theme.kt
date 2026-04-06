package com.ryuken.obsidianledger.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════════
// Extended Ledger Color Tokens — CompositionLocal
// ═══════════════════════════════════════════════════════════════════════

@Immutable
data class LedgerColors(
    val surfaceBase       : Color,
    val surfaceLow        : Color,
    val surfaceContainer  : Color,
    val surfaceHigh       : Color,
    val surfaceHighest    : Color,
    val interactive       : Color,
    val accentStart       : Color,
    val accentEnd         : Color,
    val danger            : Color,
    val dangerContainer   : Color,
    val warning           : Color,
    val warningContainer  : Color,
    val ghostBorder       : Color,
    val onSurfacePrimary  : Color,
    val onSurfaceSecondary: Color,
)

private val DarkLedgerColors = LedgerColors(
    surfaceBase        = DarkBackground,
    surfaceLow         = DarkSurface,
    surfaceContainer   = DarkSurfaceContainer,
    surfaceHigh        = DarkSurfaceHigh,
    surfaceHighest     = DarkSurfaceHighest,
    interactive        = DarkInteractive,
    accentStart        = EmeraldDark,
    accentEnd          = EmeraldLight,
    danger             = Danger,
    dangerContainer    = DangerContainer,
    warning            = Warning,
    warningContainer   = WarningContainer,
    ghostBorder        = GhostBorderDark,
    onSurfacePrimary   = DarkOnSurface,
    onSurfaceSecondary = DarkOnSurfaceVariant,
)

private val LightLedgerColors = LedgerColors(
    surfaceBase        = LightBackground,
    surfaceLow         = LightSurface,
    surfaceContainer   = LightSurfaceContainer,
    surfaceHigh        = LightSurfaceHigh,
    surfaceHighest     = LightSurfaceHighest,
    interactive        = LightInteractive,
    accentStart        = EmeraldLight,
    accentEnd          = EmeraldDark,
    danger             = Danger,
    dangerContainer    = Color(0xFFFFE5E5),
    warning            = Warning,
    warningContainer   = Color(0xFFFFF3D0),
    ghostBorder        = GhostBorderLight,
    onSurfacePrimary   = LightOnSurface,
    onSurfaceSecondary = LightOnSurfaceVariant,
)

internal val LocalLedgerColors = staticCompositionLocalOf { DarkLedgerColors }

// ═══════════════════════════════════════════════════════════════════════
// LedgerTheme accessor object
// ═══════════════════════════════════════════════════════════════════════

object LedgerTheme {
    val colors: LedgerColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLedgerColors.current

    val currencySymbol: String
        @Composable
        @ReadOnlyComposable
        get() = LocalCurrencySymbol.current
}

// ═══════════════════════════════════════════════════════════════════════
// Material3 Color Scheme mappings
// ═══════════════════════════════════════════════════════════════════════

private val ObsidianDarkColorScheme = darkColorScheme(
    primary                = EmeraldDark,
    onPrimary              = OnEmerald,
    primaryContainer       = EmeraldContainer,
    onPrimaryContainer     = EmeraldDark,
    secondary              = DarkOnSurfaceVariant,
    onSecondary            = DarkBackground,
    secondaryContainer     = DarkSurfaceContainer,
    onSecondaryContainer   = DarkOnSurface,
    tertiary               = EmeraldSubtle,
    onTertiary             = EmeraldDark,
    tertiaryContainer      = DarkSurfaceHigh,
    onTertiaryContainer    = DarkOnSurface,
    error                  = Danger,
    onError                = OnDanger,
    errorContainer         = DangerContainer,
    onErrorContainer       = Danger,
    background             = DarkBackground,
    onBackground           = DarkOnSurface,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceContainer,
    onSurfaceVariant       = DarkOnSurfaceVariant,
    outline                = DarkOutline,
    outlineVariant         = DarkOutlineVariant,
    scrim                  = Scrim,
    inverseSurface         = DarkOnSurface,
    inverseOnSurface       = DarkBackground,
    inversePrimary         = EmeraldLight,
    surfaceDim             = DarkBackground,
    surfaceBright          = DarkSurfaceHigh,
    surfaceContainerLowest = Color(0xFF050508),
    surfaceContainerLow    = DarkSurface,
    surfaceContainer       = DarkSurfaceContainer,
    surfaceContainerHigh   = DarkSurfaceHigh,
    surfaceContainerHighest= DarkSurfaceHighest,
)

private val ObsidianLightColorScheme = lightColorScheme(
    primary                = EmeraldLight,
    onPrimary              = Color.White,
    primaryContainer       = Color(0xFFB8F5E1),
    onPrimaryContainer     = EmeraldLight,
    secondary              = LightOnSurfaceVariant,
    onSecondary            = Color.White,
    secondaryContainer     = LightSurfaceContainer,
    onSecondaryContainer   = LightOnSurface,
    tertiary               = Color(0xFFE0F7EF),
    onTertiary             = EmeraldLight,
    tertiaryContainer      = LightSurfaceHigh,
    onTertiaryContainer    = LightOnSurface,
    error                  = Danger,
    onError                = OnDanger,
    errorContainer         = Color(0xFFFFE5E5),
    onErrorContainer       = Danger,
    background             = LightBackground,
    onBackground           = LightOnSurface,
    surface                = LightSurface,
    onSurface              = LightOnSurface,
    surfaceVariant         = LightSurfaceContainer,
    onSurfaceVariant       = LightOnSurfaceVariant,
    outline                = LightOutline,
    outlineVariant         = LightOutlineVariant,
    scrim                  = Scrim,
    inverseSurface         = LightOnSurface,
    inverseOnSurface       = LightBackground,
    inversePrimary         = EmeraldDark,
    surfaceDim             = LightSurfaceHigh,
    surfaceBright          = LightBackground,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow    = LightBackground,
    surfaceContainer       = LightSurfaceContainer,
    surfaceContainerHigh   = LightSurfaceHigh,
    surfaceContainerHighest= LightSurfaceHighest,
)

// ═══════════════════════════════════════════════════════════════════════
// Shapes — 8dp standard, 12dp large (cards/sheets)
// ═══════════════════════════════════════════════════════════════════════

private val ObsidianShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(8.dp),
    large      = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

// ═══════════════════════════════════════════════════════════════════════
// Dark mode state
// ═══════════════════════════════════════════════════════════════════════

internal val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }

// ═══════════════════════════════════════════════════════════════════════
// Root theme composable
// ═══════════════════════════════════════════════════════════════════════

object LedgerThemeConfig {
    val themeFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>("System")
}

object LedgerCurrencyConfig {
    val currencyFlow = kotlinx.coroutines.flow.MutableStateFlow<String>("₹")
}

// ═══════════════════════════════════════════════════════════════════════
// Root theme composable
// ═══════════════════════════════════════════════════════════════════════

internal val LocalCurrencySymbol = staticCompositionLocalOf { "₹" }

@Composable
internal fun AppTheme(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val themePref by LedgerThemeConfig.themeFlow.collectAsState()
    
    val actualIsDark = when (themePref) {
        "Dark" -> true
        "Light" -> false
        else -> systemIsDark
    }

    val isDarkState = remember(actualIsDark) { mutableStateOf(actualIsDark) }

    CompositionLocalProvider(
        LocalThemeIsDark provides isDarkState
    ) {
        val isDark by isDarkState
        onThemeChanged(isDark)

        val colorScheme = if (isDark) ObsidianDarkColorScheme else ObsidianLightColorScheme
        val ledgerColors = if (isDark) DarkLedgerColors else LightLedgerColors
        val typography = LedgerTypography()
        
        val currencySymbol by LedgerCurrencyConfig.currencyFlow.collectAsState()

        CompositionLocalProvider(
            LocalLedgerColors provides ledgerColors,
            LocalCurrencySymbol provides currencySymbol
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography  = typography,
                shapes      = ObsidianShapes,
                content     = {
                    Surface(
                        color = colorScheme.background,
                        content = content
                    )
                }
            )
        }
    }
}
