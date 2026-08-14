package com.mcc.construction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors

/**
 * The workhorse list item, used for tasks, projects, employees, materials,
 * documents and messages alike.
 *
 * Text is capped at three tiers — title, subtitle, meta — each a step quieter
 * than the last. Every list in the app therefore has the same rhythm even though
 * the screens were written separately.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    var base = modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (onClick != null) base = base.clickable(onClick = onClick)

    Row(
        modifier = base
            .heightIn(min = Spacing.rowHeight)
            .padding(horizontal = Spacing.cardPad, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Spacing.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (meta != null) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Spacing.md))
            trailing()
        }
    }
}

/** A square tinted icon container, used as the leading slot on non-people rows. */
@Composable
fun LeadingIcon(
    icon: ImageVector,
    tone: Tone = Tone.Info,
    modifier: Modifier = Modifier
) {
    val c = tone.colors()
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(c.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.ink,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Right-aligned stack for a pill above a timestamp. */
@Composable
fun TrailingStack(content: @Composable ColumnScope.() -> Unit) {
    Column(horizontalAlignment = Alignment.End, content = content)
}

/** Convenience wrapper so callers can put a chevron in the trailing slot. */
@Composable
fun RowScope.Grow() {
    Spacer(Modifier.weight(1f))
}
