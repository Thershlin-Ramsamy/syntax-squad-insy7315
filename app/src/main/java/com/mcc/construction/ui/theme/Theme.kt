package com.mcc.construction.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = BrandWhite,
    primaryContainer = NavyBluePrimaryContainer,
    onPrimaryContainer = OnNavyBluePrimaryContainer,
    secondary = ConstructionOrange,
    onSecondary = OnConstructionOrange,
    secondaryContainer = OrangeSecondaryContainer,
    onSecondaryContainer = OnOrangeSecondaryContainer,
    tertiary = SuccessGreen,
    onTertiary = BrandWhite,
    error = DangerRed,
    onError = BrandWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BrandWhite,
    onBackground = OnSurfaceHighContrast,
    surface = BrandWhite,
    onSurface = OnSurfaceHighContrast,
    surfaceVariant = CardGrey,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = NavyBlueDark,
    onPrimary = OnNavyBlueDark,
    primaryContainer = NavyBluePrimaryContainerDark,
    onPrimaryContainer = NavyBluePrimaryContainer,
    secondary = ConstructionOrange,
    onSecondary = OnConstructionOrange,
    secondaryContainer = OrangeSecondaryContainerDark,
    onSecondaryContainer = OnOrangeSecondaryContainerDark,
    tertiary = SuccessGreen,
    onTertiary = OnSurfaceHighContrast,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = DangerRed,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = BackgroundDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

// Rounded corners throughout, per CLAUDE.md design notes.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MCCConstructionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is intentionally not offered: this is a branded corporate
    // app and must always show MCC's navy/orange palette, not the device wallpaper theme.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
