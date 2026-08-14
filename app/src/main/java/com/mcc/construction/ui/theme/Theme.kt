package com.mcc.construction.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Elevation policy.
 *
 * Cards and list rows sit flat on the surface and are separated by a hairline
 * border plus a subtle fill, never by a drop shadow. Shadows are reserved for
 * things that genuinely float above the page — app bars once scrolled, dialogs,
 * bottom sheets, the FAB.
 *
 * Stacked translucent shadows are the single strongest "stock Material" tell,
 * and on a dense list screen they turn into visual mud. Flat surfaces with
 * crisp 1dp separators also survive glare far better outdoors.
 */

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = BrandWhite,
    primaryContainer = NavyContainer,
    onPrimaryContainer = InfoInk,
    inversePrimary = Navy300,

    secondary = ConstructionOrange,
    onSecondary = Ink900,
    secondaryContainer = WarningContainer,
    onSecondaryContainer = WarningInk,

    tertiary = SuccessGreen,
    onTertiary = BrandWhite,
    tertiaryContainer = SuccessContainer,
    onTertiaryContainer = SuccessInk,

    error = DangerRed,
    onError = BrandWhite,
    errorContainer = DangerContainer,
    onErrorContainer = DangerInk,

    background = BrandWhite,
    onBackground = Ink900,
    surface = BrandWhite,
    onSurface = Ink900,
    surfaceVariant = CardGrey,
    onSurfaceVariant = Ink500,
    surfaceContainerLowest = BrandWhite,
    surfaceContainerLow = SurfaceSubtle,
    surfaceContainer = CardGrey,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,

    outline = LineStrong,
    outlineVariant = Line,
    scrim = Ink900
)

private val DarkColorScheme = darkColorScheme(
    primary = Navy300,
    onPrimary = Navy900,
    primaryContainer = InfoContainerDark,
    onPrimaryContainer = InfoInkDark,
    inversePrimary = NavyBlue,

    secondary = ConstructionOrange,
    onSecondary = OnOrangeInk,
    secondaryContainer = WarningContainerDark,
    onSecondaryContainer = WarningInkDark,

    tertiary = SuccessFigure,
    onTertiary = OnSuccessInk,
    tertiaryContainer = SuccessContainerDark,
    onTertiaryContainer = SuccessInkDark,

    error = DangerInkDark,
    onError = OnDangerInk,
    errorContainer = DangerContainerDark,
    onErrorContainer = DangerInkDark,

    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkBackground,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkInkMuted,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceRaised,
    surfaceContainerHighest = DarkSurfaceRaised,

    outline = DarkLine,
    outlineVariant = DarkLine,
    scrim = Color.Black
)

/**
 * Corner radii. Generous but not pill-shaped: 14dp on cards reads as modern
 * without the toy-like feel of very large radii on dense data surfaces.
 * Buttons and pills use [Shapes.large] / full rounding for a clear affordance.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun MCCConstructionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic colour is deliberately not offered. This is a branded corporate
    // app: MCC's navy/orange must survive whatever wallpaper the device has.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
