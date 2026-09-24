package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.text
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors

/**
 * Wraps a screen and shows its [UiMessage] as a snackbar along the bottom, so
 * every action gets visible feedback — saved, submitted, queued offline, or
 * what went wrong.
 *
 * ```
 * ActionFeedbackHost(state.message, onMessageShown = viewModel::onMessageShown) {
 *     ScreenScaffold { … }
 * }
 * ```
 *
 * Errors use the danger tone so they are not mistaken for confirmations.
 */
@Composable
fun ActionFeedbackHost(
    message: UiMessage?,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val hostState = remember { SnackbarHostState() }
    val text = message?.text()
    val currentOnShown = rememberUpdatedState(onMessageShown)

    LaunchedEffect(message?.id) {
        if (message != null && text != null) {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(text, duration = SnackbarDuration.Short)
            currentOnShown.value()
        }
    }

    Box(modifier.fillMaxSize()) {
        content()
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = Spacing.formMaxWidth)
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) { data ->
            val tone = if (message?.isError == true) Tone.Danger else Tone.Info
            val errorColors = tone.colors()
            if (message?.isError == true) {
                Snackbar(
                    snackbarData = data,
                    containerColor = errorColors.container,
                    contentColor = errorColors.ink
                )
            } else {
                Snackbar(snackbarData = data)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
private fun ActionFeedbackPreview() {
    PreviewSurface {
        Box(Modifier.fillMaxSize()) {
            Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Text("Clocked in at 07:02")
            }
        }
    }
}
