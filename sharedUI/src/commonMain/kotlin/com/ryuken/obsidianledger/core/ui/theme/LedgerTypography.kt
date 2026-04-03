package com.ryuken.obsidianledger.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import obsidian_ledger.sharedui.generated.resources.Inter_Medium
import obsidian_ledger.sharedui.generated.resources.Inter_Regular
import obsidian_ledger.sharedui.generated.resources.Res
import obsidian_ledger.sharedui.generated.resources.SpaceGrotesk_Bold
import obsidian_ledger.sharedui.generated.resources.SpaceGrotesk_Medium
import obsidian_ledger.sharedui.generated.resources.SpaceGrotesk_Regular
import obsidian_ledger.sharedui.generated.resources.SpaceGrotesk_SemiBold
import org.jetbrains.compose.resources.Font

// ═══════════════════════════════════════════════════════════════════════
// Font Families
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SpaceGroteskFamily(): FontFamily = FontFamily(
    Font(Res.font.SpaceGrotesk_Regular,  weight = FontWeight.Normal),
    Font(Res.font.SpaceGrotesk_Medium,   weight = FontWeight.Medium),
    Font(Res.font.SpaceGrotesk_SemiBold, weight = FontWeight.SemiBold),
    Font(Res.font.SpaceGrotesk_Bold,     weight = FontWeight.Bold)
)

@Composable
fun InterFamily(): FontFamily = FontFamily(
    Font(Res.font.Inter_Regular, weight = FontWeight.Normal),
    Font(Res.font.Inter_Medium,  weight = FontWeight.Medium)
)

// ═══════════════════════════════════════════════════════════════════════
// Typography factory — called inside @Composable context
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun LedgerTypography(): Typography {
    val spaceGrotesk = SpaceGroteskFamily()
    val inter = InterFamily()

    return Typography(
        // ── Display (hero numbers) ────────────────────────────────
        displayLarge = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.Bold,
            fontSize     = 40.sp,
            lineHeight   = 48.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.Bold,
            fontSize     = 34.sp,
            lineHeight   = 40.sp,
            letterSpacing = (-0.3).sp
        ),
        displaySmall = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.SemiBold,
            fontSize     = 28.sp,
            lineHeight   = 34.sp
        ),

        // ── Headlines ─────────────────────────────────────────────
        headlineLarge = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.SemiBold,
            fontSize     = 24.sp,
            lineHeight   = 30.sp
        ),
        headlineMedium = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.Medium,
            fontSize     = 20.sp,
            lineHeight   = 26.sp
        ),
        headlineSmall = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.Medium,
            fontSize     = 18.sp,
            lineHeight   = 24.sp
        ),

        // ── Titles ────────────────────────────────────────────────
        titleLarge = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.SemiBold,
            fontSize     = 20.sp,
            lineHeight   = 26.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.Medium,
            fontSize     = 16.sp,
            lineHeight   = 22.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily   = spaceGrotesk,
            fontWeight   = FontWeight.Medium,
            fontSize     = 14.sp,
            lineHeight   = 20.sp,
            letterSpacing = 0.1.sp
        ),

        // ── Body (Inter) ──────────────────────────────────────────
        bodyLarge = TextStyle(
            fontFamily   = inter,
            fontWeight   = FontWeight.Normal,
            fontSize     = 16.sp,
            lineHeight   = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily   = inter,
            fontWeight   = FontWeight.Normal,
            fontSize     = 14.sp,
            lineHeight   = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily   = inter,
            fontWeight   = FontWeight.Normal,
            fontSize     = 12.sp,
            lineHeight   = 16.sp,
            letterSpacing = 0.4.sp
        ),

        // ── Labels (Space Grotesk, architectural signage feel) ────
        labelLarge = TextStyle(
            fontFamily      = spaceGrotesk,
            fontWeight      = FontWeight.Medium,
            fontSize        = 14.sp,
            lineHeight      = 20.sp,
            letterSpacing   = 1.2.sp
        ),
        labelMedium = TextStyle(
            fontFamily      = spaceGrotesk,
            fontWeight      = FontWeight.Medium,
            fontSize        = 12.sp,
            lineHeight      = 16.sp,
            letterSpacing   = 1.2.sp
        ),
        labelSmall = TextStyle(
            fontFamily      = spaceGrotesk,
            fontWeight      = FontWeight.Medium,
            fontSize        = 10.sp,
            lineHeight      = 14.sp,
            letterSpacing   = 1.2.sp
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Amount text style — tabular numerals for aligned currency figures
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun AmountTextStyle(fontSize: Float = 40f): TextStyle {
    val spaceGrotesk = SpaceGroteskFamily()
    return TextStyle(
        fontFamily          = spaceGrotesk,
        fontWeight          = FontWeight.Bold,
        fontSize            = fontSize.sp,
        lineHeight          = (fontSize * 1.2).sp,
        fontFeatureSettings = "tnum",
        letterSpacing       = (-0.5).sp
    )
}

@Composable
fun TabularStyle(fontSize: Float = 16f, fontWeight: FontWeight = FontWeight.Medium): TextStyle {
    val spaceGrotesk = SpaceGroteskFamily()
    return TextStyle(
        fontFamily          = spaceGrotesk,
        fontWeight          = fontWeight,
        fontSize            = fontSize.sp,
        fontFeatureSettings = "tnum"
    )
}
