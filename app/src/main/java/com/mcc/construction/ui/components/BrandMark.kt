package com.mcc.construction.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mcc.construction.ui.theme.ConstructionOrange

/**
 * The launcher mark, redrawn in Compose so the splash and sign-in screens show
 * the same hard hat as the home-screen icon rather than a stand-in glyph.
 *
 * Proportions are identical to `ic_launcher_foreground.xml`: brim width W drives
 * everything, and the mark is vertically centred by construction.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    domeColor: Color = Color.White,
    grooveColor: Color,
    brimColor: Color = ConstructionOrange
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val cx = size.width / 2f
        val cy = size.height / 2f

        val brimH = w * 0.17f
        val brimTop = cy + w * 0.075f
        val domeR = w * 0.32f
        val slotW = w * 0.13f

        // Dome — top half of a circle centred on the brim line
        drawArc(
            color = domeColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - domeR, brimTop - domeR),
            size = Size(domeR * 2, domeR * 2)
        )

        // Crown groove, cut through the dome and into the brim
        val slotTop = brimTop - domeR * 0.72f
        drawRoundRect(
            color = grooveColor,
            topLeft = Offset(cx - slotW / 2f, slotTop),
            size = Size(slotW, (brimTop + brimH) - slotTop),
            cornerRadius = CornerRadius(slotW / 2f, slotW / 2f)
        )

        // Brim, drawn last so it closes the dome and groove cleanly
        drawRoundRect(
            color = brimColor,
            topLeft = Offset(cx - w / 2f, brimTop),
            size = Size(w, brimH),
            cornerRadius = CornerRadius(brimH / 2f, brimH / 2f)
        )
    }
}
