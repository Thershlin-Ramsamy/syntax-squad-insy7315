package com.mcc.construction.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.window.core.layout.WindowSizeClass

/** Material 3 width buckets: phone, small tablet / folded, desktop / large tablet. */
enum class WindowWidthClass { Compact, Medium, Expanded }

/**
 * How much room the app window has, read once at the root and handed down
 * through [LocalWindowLayout].
 *
 * Screens adapt by reading this rather than measuring for themselves, so every
 * screen switches layout at the same breakpoints:
 *
 * ```
 * val layout = LocalWindowLayout.current
 * if (layout.isExpanded) TwoColumnLayout() else SingleColumnLayout()
 * ```
 */
@Immutable
data class WindowLayout(
    val widthClass: WindowWidthClass,
    val heightDp: Int
) {
    val isCompact: Boolean get() = widthClass == WindowWidthClass.Compact
    val isExpanded: Boolean get() = widthClass == WindowWidthClass.Expanded

    companion object {
        /** A typical phone in portrait — the default outside the app root. */
        val Phone = WindowLayout(WindowWidthClass.Compact, heightDp = 891)
    }
}

val LocalWindowLayout = staticCompositionLocalOf { WindowLayout.Phone }

/**
 * Reads the current window through Material 3 Adaptive.
 *
 * @param forcedWidthClass overrides the measured width. Used only by the
 *   screenshot tooling, which cannot resize some emulators.
 */
@Composable
fun rememberWindowLayout(forcedWidthClass: WindowWidthClass? = null): WindowLayout {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val widthClass = forcedWidthClass ?: when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
            WindowWidthClass.Expanded

        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            WindowWidthClass.Medium

        else -> WindowWidthClass.Compact
    }

    val containerHeightPx = LocalWindowInfo.current.containerSize.height
    val heightDp = with(LocalDensity.current) { containerHeightPx.toDp().value.toInt() }

    return remember(widthClass, heightDp) { WindowLayout(widthClass, heightDp) }
}
