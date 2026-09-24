package com.mcc.construction.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.SwitchRow
import com.mcc.construction.ui.components.syncStateLabel
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone

/** Settings wired to its ViewModel. */
@Composable
fun SettingsRoute(
    onOpenSyncStatus: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onNotificationsChange = viewModel::onNotificationsChange,
        onVibrationChange = viewModel::onVibrationChange,
        onOfflineModeChange = viewModel::onOfflineModeChange,
        onOpenSyncStatus = onOpenSyncStatus
    )
}

/**
 * Notifications, offline mode and app details. Every toggle is a full-width
 * row, so it can be flipped with a gloved thumb.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onNotificationsChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onOfflineModeChange: (Boolean) -> Unit,
    onOpenSyncStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings_section_notifications))
        Spacer(Modifier.height(Spacing.blockGap))
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SwitchRow(
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(R.string.settings_notifications_subtitle),
                icon = Icons.Filled.Notifications,
                tone = Tone.Info,
                checked = state.notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = stringResource(R.string.settings_vibrate),
                subtitle = stringResource(R.string.settings_vibrate_subtitle),
                icon = Icons.Filled.Vibration,
                checked = state.vibrationEnabled && state.notificationsEnabled,
                enabled = state.notificationsEnabled,
                onCheckedChange = onVibrationChange
            )
        }

        Spacer(Modifier.height(Spacing.sectionGap))

        SectionHeader(title = stringResource(R.string.settings_section_data))
        Spacer(Modifier.height(Spacing.blockGap))
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SwitchRow(
                title = stringResource(R.string.settings_offline_mode),
                subtitle = stringResource(R.string.settings_offline_mode_subtitle),
                icon = Icons.Filled.CloudOff,
                tone = if (state.isOfflineMode) Tone.Danger else Tone.Neutral,
                checked = state.isOfflineMode,
                onCheckedChange = onOfflineModeChange
            )
        }
        Spacer(Modifier.height(Spacing.itemGap))
        ListRow(
            title = stringResource(R.string.title_sync_status),
            subtitle = syncStateLabel(state.syncStatus.state, state.syncStatus.pendingCount),
            onClick = onOpenSyncStatus,
            leading = { LeadingIcon(state.syncStatus.state.icon, state.syncStatus.state.tone) },
            trailing = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Spacer(Modifier.height(Spacing.sectionGap))

        SectionHeader(title = stringResource(R.string.settings_section_about))
        Spacer(Modifier.height(Spacing.blockGap))
        AppCard(modifier = Modifier.fillMaxWidth()) {
            KeyValueRow(stringResource(R.string.settings_app_version), state.appVersion)
            Spacer(Modifier.height(Spacing.md))
            state.signedInRole?.let { role ->
                KeyValueRow(stringResource(R.string.settings_signed_in_as), stringResource(role.labelRes))
                Spacer(Modifier.height(Spacing.md))
            }
            KeyValueRow(
                stringResource(R.string.settings_data_source),
                stringResource(
                    if (state.isDemoData) R.string.settings_data_source_demo
                    else R.string.settings_data_source_server
                )
            )
        }
    }
}

@DevicePreviews
@Composable
private fun SettingsScreenPreview() {
    PreviewSurface {
        SettingsScreen(
            state = SettingsUiState(
                syncStatus = SyncStatus.Initial.copy(isOfflineMode = true),
                signedInRole = UserRole.WORKER,
                appVersion = "1.0 (1)",
                isDemoData = true
            ),
            onNotificationsChange = {},
            onVibrationChange = {},
            onOfflineModeChange = {},
            onOpenSyncStatus = {}
        )
    }
}
