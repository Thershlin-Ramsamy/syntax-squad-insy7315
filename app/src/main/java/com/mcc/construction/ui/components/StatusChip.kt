package com.mcc.construction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors

/**
 * A large, high-contrast status label — the one to use when the status is the
 * point of the screen (a task's state on its detail page, a project's health
 * on its header), or anywhere it must read at arm's length outdoors.
 *
 * For dense lists, the smaller [StatusPill] fits better. Both take a [Tone], so
 * pick the tone from `ui/ModelStyle.kt` rather than choosing a colour:
 *
 *     StatusChip(stringResource(task.status.labelRes), task.status.tone)
 *
 * Without an [icon] a coloured dot leads the text, so the status never relies
 * on the word alone.
 */
@Composable
fun StatusChip(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val c = tone.colors()
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .background(c.container)
            .padding(horizontal = 14.dp, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.ink,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(c.figure)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = c.ink,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, widthDp = 240)
@Composable
private fun StatusChipPreview() {
    PreviewSurface {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatusChip("In progress", Tone.Info)
            StatusChip("Completed", Tone.Success, icon = Icons.Filled.CheckCircle)
            StatusChip("Low stock", Tone.Warning)
            StatusChip("Blocked", Tone.Danger, icon = Icons.Filled.Block)
            StatusChip("Planning", Tone.Neutral)
        }
    }
}
