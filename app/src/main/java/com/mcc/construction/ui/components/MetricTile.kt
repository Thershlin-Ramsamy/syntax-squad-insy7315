package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors

/**
 * A single KPI. The number is the loudest thing on the tile and the label is
 * deliberately quiet — dashboards are scanned, not read, so the figure has to
 * survive a half-second glance.
 */
@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Tone = Tone.Neutral,
    caption: String? = null,
    onClick: (() -> Unit)? = null
) {
    val c = tone.colors()
    AppCard(modifier = modifier, onClick = onClick) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.figure,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(Spacing.sm))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (caption != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = c.ink
            )
        }
    }
}

/**
 * A compact metric used where a full tile would be too heavy — inside a detail
 * card, or in a strip of four or more figures.
 */
@Composable
fun MetricInline(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Neutral
) {
    val c = tone.colors()
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (tone == Tone.Neutral) MaterialTheme.colorScheme.onSurface else c.ink
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Evenly distributes a small set of inline metrics across a card. */
@Composable
fun MetricStrip(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        content = content
    )
}
