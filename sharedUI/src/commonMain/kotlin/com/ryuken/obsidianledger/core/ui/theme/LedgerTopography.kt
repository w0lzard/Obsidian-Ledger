package com.ryuken.obsidianledger.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import obsidian_ledger.sharedui.generated.resources.Inter_Medium
import obsidian_ledger.sharedui.generated.resources.Inter_Regular
import obsidian_ledger.sharedui.generated.resources.Res
import obsidian_ledger.sharedui.generated.resources.SpaceGrotesk_Bold
import obsidian_ledger.sharedui.generated.resources.SpaceGrotesk_Medium
import obsidian_ledger.sharedui.generated.resources.SpaceGrotesk_Regular
import obsidian_ledger.sharedui.generated.resources.SpaceGrotesk_SemiBold
import org.jetbrains.compose.resources.Font

val SpaceGrotesk = FontFamily(
    Font(Res.font.SpaceGrotesk_Regular,  weight = FontWeight.Normal),
    Font(Res.font.SpaceGrotesk_Medium,   weight = FontWeight.Medium),
    Font(Res.font.SpaceGrotesk_SemiBold, weight = FontWeight.SemiBold),
    Font(Res.font.SpaceGrotesk_Bold,     weight = FontWeight.Bold)
)

val Inter = FontFamily(
    Font(Res.font.Inter_Regular, weight = FontWeight.Normal),
    Font(Res.font.Inter_Medium,  weight = FontWeight.Medium)
)
