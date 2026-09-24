package com.mcc.construction.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Colour system.
 *
 * The six brand tokens from the brand guide are the fixed points. Navy and gold are
 * sampled directly from the client's logo, so the app and the printed lockup
 * match. Everything else is a derived neutral ramp or a semantic "tone" pair.
 *
 * Two rules govern usage, both driven by the fact that this app is read outdoors
 * on a construction site:
 *
 *  1. Brand hues are never used as text on white. Navy is dark enough at ~12.9:1,
 *     but raw gold (#DEAC2D) sits at ~2.1:1 and green (#28A745) at ~2.9:1 against
 *     white — both fail WCAG AA. Each tone therefore carries a saturated "figure"
 *     colour for fills, dots and icons, and a darkened "ink" colour for text.
 *  2. Colour carries meaning only. Navy = structure and primary action, gold =
 *     the single most important call to action or an attention state, green/red =
 *     status. Nothing is coloured decoratively.
 */

// ---------------------------------------------------------------------------
// Brand tokens — navy and gold sampled from the MCC logo
// ---------------------------------------------------------------------------
// These six values are the source of truth for the brand. Screens should not
// reference them directly: use MaterialTheme.colorScheme (primary = navy,
// secondary = gold) or a Tone, so dark theme and status colours stay correct.
val NavyBlue = Color(0xFF173061) // Primary
val ConstructionGold = Color(0xFFDEAC2D) // Accent
val BrandWhite = Color(0xFFFFFFFF) // Background
val CardGrey = Color(0xFFF4F4F4) // Card background
val SuccessGreen = Color(0xFF28A745) // Success
val DangerRed = Color(0xFFDC3545) // Error / danger

// ---------------------------------------------------------------------------
// Neutral ramp — high contrast for outdoor legibility
// ---------------------------------------------------------------------------
val Ink900 = Color(0xFF0F1419) // primary text
val Ink700 = Color(0xFF2B333D)
val Ink500 = Color(0xFF56606D) // secondary text
val Ink400 = Color(0xFF78828F) // meta / tertiary text
val Line = Color(0xFFE3E7EC) // hairline borders
val LineStrong = Color(0xFFCFD5DD)
val SurfaceSubtle = Color(0xFFF7F9FB) // page-level wash behind cards
val SurfaceHigh = Color(0xFFF0F2F5)
val SurfaceHighest = Color(0xFFE9ECF0)

// "On" colours for saturated brand fills
val OnGoldInk = Color(0xFF231B03)
val OnSuccessInk = Color(0xFF00210B)
val OnDangerInk = Color(0xFF5F1219)

// Navy ramp
val Navy900 = Color(0xFF0D1D3C)
val Navy700 = Color(0xFF173061)
val Navy300 = Color(0xFF9DB4DC)
val NavyContainer = Color(0xFFE7ECF4)

// ---------------------------------------------------------------------------
// Semantic tone pairs — container fill, figure (dots/icons/fills), ink (text)
// ---------------------------------------------------------------------------
val InfoContainer = NavyContainer
val InfoFigure = NavyBlue
val InfoInk = Color(0xFF142A55)

val SuccessContainer = Color(0xFFE6F4EA)
val SuccessFigure = SuccessGreen
val SuccessInk = Color(0xFF1B7A32)

val WarningContainer = Color(0xFFFBF2DC)
val WarningFigure = ConstructionGold
val WarningInk = Color(0xFF8A6910)

val DangerContainer = Color(0xFFFBE9EB)
val DangerFigure = DangerRed
val DangerInk = Color(0xFFB3202E)

val NeutralContainer = Color(0xFFEFF2F5)
val NeutralFigure = Ink400
val NeutralInk = Color(0xFF48515C)

// ---------------------------------------------------------------------------
// Dark theme
// ---------------------------------------------------------------------------
val DarkBackground = Color(0xFF0E1116)
val DarkSurface = Color(0xFF151A21)
val DarkSurfaceRaised = Color(0xFF1D242D)
val DarkLine = Color(0xFF2C343E)
val DarkInk = Color(0xFFECEFF3)
val DarkInkMuted = Color(0xFFA3ADBA)

val InfoContainerDark = Color(0xFF16294B)
val InfoInkDark = Color(0xFFBCCDE8)
val SuccessContainerDark = Color(0xFF14361F)
val SuccessInkDark = Color(0xFF7FD99A)
val WarningContainerDark = Color(0xFF3B2E0A)
val WarningInkDark = Color(0xFFEDC96F)
val DangerContainerDark = Color(0xFF43151B)
val DangerInkDark = Color(0xFFFF9AA4)
val NeutralContainerDark = Color(0xFF222A33)
val NeutralInkDark = Color(0xFFB6C0CB)
