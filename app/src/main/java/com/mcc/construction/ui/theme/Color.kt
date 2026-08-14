package com.mcc.construction.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Colour system.
 *
 * The six brand tokens from CLAUDE.md are the fixed points. Everything else is a
 * derived neutral ramp or a semantic "tone" pair.
 *
 * Two rules govern usage, both driven by the fact that this app is read outdoors
 * on a construction site:
 *
 *  1. Brand hues are never used as text on white. Navy is dark enough, but raw
 *     orange (#F9A826) sits at ~1.9:1 and green (#28A745) at ~2.9:1 against white —
 *     both fail WCAG AA. Each tone therefore carries a saturated "figure" colour
 *     for fills, dots and icons, and a darkened "ink" colour for text.
 *  2. Colour carries meaning only. Navy = structure and primary action, orange =
 *     the single most important call to action or an attention state, green/red =
 *     status. Nothing is coloured decoratively.
 */

// ---------------------------------------------------------------------------
// Brand tokens (CLAUDE.md)
// ---------------------------------------------------------------------------
val NavyBlue = Color(0xFF0B3D91)
val ConstructionOrange = Color(0xFFF9A826)
val BrandWhite = Color(0xFFFFFFFF)
val CardGrey = Color(0xFFF4F4F4)
val SuccessGreen = Color(0xFF28A745)
val DangerRed = Color(0xFFDC3545)

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
val OnOrangeInk = Color(0xFF241A00)
val OnSuccessInk = Color(0xFF00210B)
val OnDangerInk = Color(0xFF5F1219)

// Navy ramp
val Navy900 = Color(0xFF06255A)
val Navy700 = Color(0xFF0B3D91)
val Navy300 = Color(0xFF9DBEFF)
val NavyContainer = Color(0xFFE8EFFB)

// ---------------------------------------------------------------------------
// Semantic tone pairs — container fill, figure (dots/icons/fills), ink (text)
// ---------------------------------------------------------------------------
val InfoContainer = NavyContainer
val InfoFigure = NavyBlue
val InfoInk = Color(0xFF0A337A)

val SuccessContainer = Color(0xFFE6F4EA)
val SuccessFigure = SuccessGreen
val SuccessInk = Color(0xFF1B7A32)

val WarningContainer = Color(0xFFFDF1DC)
val WarningFigure = ConstructionOrange
val WarningInk = Color(0xFF8A5A00)

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

val InfoContainerDark = Color(0xFF14325F)
val InfoInkDark = Color(0xFFBFD4FF)
val SuccessContainerDark = Color(0xFF14361F)
val SuccessInkDark = Color(0xFF7FD99A)
val WarningContainerDark = Color(0xFF3D2B08)
val WarningInkDark = Color(0xFFF7C46C)
val DangerContainerDark = Color(0xFF43151B)
val DangerInkDark = Color(0xFFFF9AA4)
val NeutralContainerDark = Color(0xFF222A33)
val NeutralInkDark = Color(0xFFB6C0CB)
