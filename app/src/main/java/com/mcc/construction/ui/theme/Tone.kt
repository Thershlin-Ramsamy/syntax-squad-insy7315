package com.mcc.construction.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * A semantic status colour, resolved into the three roles a status actually needs:
 * a [container] fill, a saturated [figure] for dots and icons, and a darkened
 * [ink] that is legible as text on the container.
 *
 * Keeping these three bound together is what stops status colour drifting from
 * screen to screen — nothing picks a raw hex for a status anywhere in the app.
 */
data class ToneColors(
    val container: Color,
    val figure: Color,
    val ink: Color
)

enum class Tone { Neutral, Info, Success, Warning, Danger }

@Composable
@ReadOnlyComposable
fun Tone.colors(): ToneColors {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Tone.Neutral -> if (dark) {
            ToneColors(NeutralContainerDark, NeutralInkDark, NeutralInkDark)
        } else {
            ToneColors(NeutralContainer, NeutralFigure, NeutralInk)
        }

        Tone.Info -> if (dark) {
            ToneColors(InfoContainerDark, InfoInkDark, InfoInkDark)
        } else {
            ToneColors(InfoContainer, InfoFigure, InfoInk)
        }

        Tone.Success -> if (dark) {
            ToneColors(SuccessContainerDark, SuccessInkDark, SuccessInkDark)
        } else {
            ToneColors(SuccessContainer, SuccessFigure, SuccessInk)
        }

        Tone.Warning -> if (dark) {
            ToneColors(WarningContainerDark, WarningInkDark, WarningInkDark)
        } else {
            ToneColors(WarningContainer, WarningFigure, WarningInk)
        }

        Tone.Danger -> if (dark) {
            ToneColors(DangerContainerDark, DangerInkDark, DangerInkDark)
        } else {
            ToneColors(DangerContainer, DangerFigure, DangerInk)
        }
    }
}
