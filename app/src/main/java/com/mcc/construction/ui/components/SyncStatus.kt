package com.mcc.construction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mcc.construction.data.MockRepository
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors

/**
 * Persistent connectivity chip shown in the app bar on every main screen.
 *
 * CLAUDE.md calls for an always-visible online/offline + sync indicator, so this
 * is deliberately present even when everything is fine rather than appearing
 * only on failure — a worker underground needs to know the state before they
 * rely on it, not after a submit silently queues.
 *
 * Tapping toggles the mock connection so the offline path can be demonstrated
 * without a real network layer.
 */
@Composable
fun SyncStatusChip(modifier: Modifier = Modifier) {
    val online by MockRepository.isOnline
    val pending by MockRepository.pendingSyncCount

    val tone = when {
        !online -> Tone.Danger
        pending > 0 -> Tone.Warning
        else -> Tone.Success
    }
    val label = when {
        !online -> "Offline"
        pending > 0 -> "$pending queued"
        else -> "Synced"
    }
    val c = tone.colors()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.container)
            .clickable { MockRepository.toggleOnline() }
            .padding(horizontal = Spacing.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(c.figure)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = c.ink
        )
    }
}
