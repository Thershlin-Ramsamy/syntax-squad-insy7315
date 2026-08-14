package com.mcc.construction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors

/**
 * Compact status label. Optionally carries a leading dot — useful when the pill
 * appears in a dense list and the colour needs to read before the word does.
 */
@Composable
fun StatusPill(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    showDot: Boolean = false
) {
    val c = tone.colors()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.container)
            .padding(horizontal = Spacing.md, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (showDot) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(c.figure)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = c.ink
        )
    }
}

/**
 * Initials avatar. The tone is derived from the name so a given person keeps the
 * same colour everywhere they appear, without anyone maintaining a mapping.
 */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val palette = listOf(Tone.Info, Tone.Success, Tone.Warning, Tone.Danger, Tone.Neutral)
    val tone = palette[(name.sumOf { it.code }) % palette.size]
    val c = tone.colors()
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(c.container),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall,
            color = c.ink,
            textAlign = TextAlign.Center
        )
    }
}
