package com.mcc.construction.ui.screens.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.OfflineSync
import com.mcc.construction.data.model.PendingChange
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.SyncOutcome
import com.mcc.construction.data.model.SyncState
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.components.SwitchRow
import com.mcc.construction.ui.format.relativeTime
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone
import java.time.LocalDateTime

/** Offline sync status wired to its ViewModel. */
@Composable
fun SyncStatusRoute(
    onBack: () -> Unit,
    viewModel: SyncStatusViewModel = viewModel(factory = SyncStatusViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SyncStatusScreen(
        state = state,
        onSyncNow = viewModel::onSyncNow,
        onOfflineModeChange = viewModel::onOfflineModeChange,
        onBack = onBack
    )
}

/**
 * Offline and sync status. Says three things plainly: whether there is a
 * connection, what is still waiting to upload, and what happens next. The
 * offline-mode switch is here so the offline path can be shown on a device
 * that is in fact connected.
 */
@Composable
fun SyncStatusScreen(
    state: SyncStatusUiState,
    onSyncNow: () -> Unit,
    onOfflineModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wide = LocalWindowLayout.current.isExpanded
    ScreenScaffold(
        modifier = modifier,
        title = stringResource(R.string.title_sync_status),
        onBack = onBack,
        maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth
    ) {
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                Column(Modifier.weight(1f)) { StatusAndActions(state, onSyncNow, onOfflineModeChange) }
                Column(Modifier.weight(1f)) { QueueAndHistory(state) }
            }
        } else {
            StatusAndActions(state, onSyncNow, onOfflineModeChange)
            Spacer(Modifier.height(Spacing.sectionGap))
            QueueAndHistory(state)
        }
    }
}

@Composable
private fun StatusAndActions(
    state: SyncStatusUiState,
    onSyncNow: () -> Unit,
    onOfflineModeChange: (Boolean) -> Unit
) {
    val status = state.status
    StatusCard(status)
    Spacer(Modifier.height(Spacing.lg))

    when {
        !status.isOnline -> Banner(
            title = stringResource(R.string.sync_offline_banner_title),
            message = stringResource(R.string.sync_offline_banner_message),
            icon = Icons.Filled.CloudOff,
            tone = Tone.Danger
        )

        state.lastUploadCount != null && status.pendingCount == 0 -> Banner(
            title = stringResource(R.string.sync_done_title),
            message = pluralStringResource(
                R.plurals.sync_done_message,
                state.lastUploadCount,
                state.lastUploadCount
            ),
            icon = Icons.Filled.CheckCircle,
            tone = Tone.Success
        )

        else -> PrimaryButton(
            text = stringResource(
                when {
                    status.isSyncing -> R.string.sync_now_running
                    status.pendingCount > 0 -> R.string.sync_now
                    else -> R.string.sync_up_to_date
                }
            ),
            icon = if (status.pendingCount > 0) Icons.Filled.CloudSync else Icons.Filled.CloudDone,
            loading = status.isSyncing,
            enabled = state.canSyncNow || status.isSyncing,
            onClick = onSyncNow
        )
    }

    Spacer(Modifier.height(Spacing.itemGap))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SwitchRow(
            title = stringResource(R.string.settings_offline_mode),
            subtitle = stringResource(R.string.sync_offline_mode_subtitle),
            icon = Icons.Filled.CloudOff,
            tone = if (status.isOfflineMode) Tone.Danger else Tone.Neutral,
            checked = status.isOfflineMode,
            onCheckedChange = onOfflineModeChange
        )
    }
}

@Composable
private fun StatusCard(status: SyncStatus) {
    val state = status.state
    val c = state.tone.colors()
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(c.container),
                contentAlignment = Alignment.Center
            ) {
                if (state == SyncState.SYNCING) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = c.ink, strokeWidth = 3.dp)
                } else {
                    Icon(state.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (status.isOnline) R.string.sync_online else R.string.sync_state_offline),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))
                // The heading already says online or offline, so the chip
                // describes the queue instead.
                StatusChip(
                    text = when {
                        status.isSyncing -> stringResource(R.string.sync_state_syncing)
                        status.pendingCount > 0 -> pluralStringResource(
                            R.plurals.sync_state_pending,
                            status.pendingCount,
                            status.pendingCount
                        )
                        else -> stringResource(R.string.sync_chip_up_to_date)
                    },
                    tone = state.tone
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        KeyValueRow(
            stringResource(R.string.sync_last_synced),
            status.lastSyncedAt?.let { relativeTime(it) } ?: stringResource(R.string.sync_never)
        )
        Spacer(Modifier.height(Spacing.md))
        KeyValueRow(stringResource(R.string.sync_waiting_to_upload), status.pendingCount.toString())
    }
}

@Composable
private fun QueueAndHistory(state: SyncStatusUiState) {
    SectionHeader(title = stringResource(R.string.sync_queue_title))
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.status.pendingChanges.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.CheckCircle,
            title = stringResource(R.string.sync_queue_empty_title),
            message = stringResource(R.string.sync_queue_empty_message),
            tone = Tone.Success
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
            state.status.pendingChanges.forEach { change -> PendingChangeRow(change, state.status.isOnline) }
        }
    }

    if (state.history.isNotEmpty()) {
        Spacer(Modifier.height(Spacing.sectionGap))
        SectionHeader(title = stringResource(R.string.sync_history_title))
        Spacer(Modifier.height(Spacing.blockGap))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
            state.history.take(HISTORY_ROWS).forEach { entry -> HistoryRow(entry) }
        }
    }
}

@Composable
private fun PendingChangeRow(change: PendingChange, online: Boolean) {
    ListRow(
        title = stringResource(change.type.labelRes),
        subtitle = change.summary.ifBlank { null },
        meta = stringResource(R.string.sync_queued_at, relativeTime(change.queuedAt)),
        leading = { LeadingIcon(change.type.icon, if (online) Tone.Warning else Tone.Danger) }
    )
}

@Composable
private fun HistoryRow(entry: OfflineSync) {
    ListRow(
        title = relativeTime(entry.timestamp),
        subtitle = entry.deviceId,
        leading = { LeadingIcon(Icons.Filled.History, Tone.Neutral) },
        trailing = { StatusPill(stringResource(entry.syncStatus.labelRes), entry.syncStatus.tone, showDot = true) }
    )
}

/** Room for two comfortable columns on a desktop-width window. */
private val WIDE_CONTENT: Dp = 1080.dp

private const val HISTORY_ROWS = 5

private val previewNow = LocalDateTime.now()

private val previewPending = listOf(
    PendingChange(1, PendingChangeType.SAFETY_INSPECTION, "Riverside Estate Phase 2", previewNow.minusMinutes(40)),
    PendingChange(2, PendingChangeType.TASK_UPDATE, "Morning safety walkaround", previewNow.minusMinutes(25)),
    PendingChange(3, PendingChangeType.CLOCK_IN, "06:52", previewNow.minusMinutes(4))
)

private val previewHistory = listOf(
    OfflineSync(1, 5, "MCC-TAB-0417", SyncOutcome.COMPLETED, previewNow.minusHours(2)),
    OfflineSync(2, 5, "MCC-TAB-0417", SyncOutcome.FAILED, previewNow.minusDays(1))
)

@DevicePreviews
@Composable
private fun SyncStatusPendingPreview() {
    PreviewSurface {
        SyncStatusScreen(
            state = SyncStatusUiState(
                status = SyncStatus(true, false, false, previewPending, previewNow.minusHours(2)),
                history = previewHistory
            ),
            onSyncNow = {},
            onOfflineModeChange = {},
            onBack = {}
        )
    }
}

@DevicePreviews
@Composable
private fun SyncStatusOfflinePreview() {
    PreviewSurface {
        SyncStatusScreen(
            state = SyncStatusUiState(
                status = SyncStatus(true, true, false, previewPending, previewNow.minusHours(2)),
                history = previewHistory
            ),
            onSyncNow = {},
            onOfflineModeChange = {},
            onBack = {}
        )
    }
}
