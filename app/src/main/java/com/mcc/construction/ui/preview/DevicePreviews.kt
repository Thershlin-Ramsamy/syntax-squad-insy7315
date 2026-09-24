package com.mcc.construction.ui.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.adaptive.rememberWindowLayout
import com.mcc.construction.ui.theme.MCCConstructionTheme

/** A phone in portrait — the compact layout. */
@Preview(
    name = "Phone",
    group = "Phone",
    device = "spec:width=411dp,height=891dp",
    showBackground = true
)
annotation class PhonePreview

/** A desktop-sized window — the expanded layout. */
@Preview(
    name = "Wide",
    group = "Wide",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    showBackground = true
)
annotation class WidePreview

/**
 * Renders a preview at both phone and wide sizes. Every screen uses this so
 * both layouts can be checked side by side without running the app.
 */
@PhonePreview
@WidePreview
annotation class DevicePreviews

/**
 * Wraps preview content in the app theme and a window layout measured from the
 * preview device, the same way the real app root does.
 */
@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    MCCConstructionTheme {
        CompositionLocalProvider(LocalWindowLayout provides rememberWindowLayout()) {
            Surface(color = MaterialTheme.colorScheme.background, content = content)
        }
    }
}
