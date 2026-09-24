package com.mcc.construction.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mcc.construction.R
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Tone

/**
 * Stand-in for a screen that has a route but has not been built yet. The
 * route, navigation item and back behaviour all work, so building the real
 * screen means replacing this call in `AppNavHost` and nothing else.
 *
 * @param onBack pass for a pushed screen; leave null for a screen inside the
 *   navigation chrome, which already has an app bar.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    ScreenScaffold(
        modifier = modifier,
        title = if (onBack != null) title else null,
        onBack = onBack
    ) {
        EmptyState(
            icon = Icons.Filled.Construction,
            title = title,
            message = stringResource(R.string.placeholder_message),
            tone = Tone.Warning
        )
    }
}

@DevicePreviews
@Composable
private fun PlaceholderScreenPreview() {
    PreviewSurface { PlaceholderScreen(title = "System settings", onBack = {}) }
}
