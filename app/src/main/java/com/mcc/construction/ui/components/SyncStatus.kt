package com.mcc.construction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mcc.construction.R
import com.mcc.construction.data.model.SyncState
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone

/**
 * What every sync indicator on screen shows, provided once by the app root so
 * that any screen — including ones pushed outside the navigation chrome — can
 * drop in [SyncStatusIndicator] without wiring a repository.
 */
@Immutable
data class SyncIndicatorState(
    val status: SyncStatus = SyncStatus.Initial,
    val onClick: (() -> Unit)? = null
)

val LocalSyncIndicatorState = compositionLocalOf { SyncIndicatorState() }

/**
 * The app-wide online / offline / syncing indicator, reading the current state
 * from [LocalSyncIndicatorState]. Tapping it opens the Offline Sync screen.
 *
 * It is always visible, even when everything is fine: a worker underground
 * needs to know the state before relying on it, not after a submit has quietly
 * gone into the queue.
 */
@Composable
fun SyncStatusIndicator(modifier: Modifier = Modifier) {
    val indicator = LocalSyncIndicatorState.current
    SyncStatusIndicator(
        state = indicator.status.state,
        pendingCount = indicator.status.pendingCount,
        onClick = indicator.onClick,
        modifier = modifier
    )
}

/**
 * Stateless form of the sync indicator, for previews and tests.
 *
 * Colour, icon and word all change together, so the state still reads for
 * colour-blind users and in direct sunlight.
 */
@Composable
fun SyncStatusIndicator(
    state: SyncState,
    pendingCount: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = state.tone.colors()
    val label = syncStateLabel(state, pendingCount)
    val description = stringResource(R.string.sync_indicator_description, label)

    var surface = modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(50))
        .background(colors.container)
    if (onClick != null) surface = surface.clickable(onClick = onClick)

    Row(
        modifier = surface
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            }
            .height(36.dp)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (state == SyncState.SYNCING) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = colors.ink,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = state.icon,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.ink,
            maxLines = 1
        )
    }
}

/** "Synced", "3 waiting", "Syncing…" or "Offline". */
@Composable
fun syncStateLabel(state: SyncState, pendingCount: Int): String = when (state) {
    SyncState.SYNCED -> stringResource(R.string.sync_state_synced)
    SyncState.PENDING -> pluralStringResource(
        R.plurals.sync_state_pending, pendingCount, pendingCount
    )
    SyncState.SYNCING -> stringResource(R.string.sync_state_syncing)
    SyncState.OFFLINE -> stringResource(R.string.sync_state_offline)
}

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun SyncStatusIndicatorPreview() {
    PreviewSurface {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SyncStatusIndicator(SyncState.SYNCED, pendingCount = 0, onClick = {})
            SyncStatusIndicator(SyncState.PENDING, pendingCount = 3, onClick = {})
            SyncStatusIndicator(SyncState.SYNCING, pendingCount = 3, onClick = {})
            SyncStatusIndicator(SyncState.OFFLINE, pendingCount = 3, onClick = {})
        }
    }
}
