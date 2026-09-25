package com.mcc.construction.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.SystemSettings
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.SwitchRow
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

@Composable
fun SystemSettingsRoute(viewModel: SystemSettingsViewModel = viewModel(factory = SystemSettingsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SystemSettingsScreen(state, viewModel::onChange, viewModel::onSave, viewModel::onDiscard, viewModel::onMessageShown)
}

/**
 * Organisation-wide settings. Changes are held as a draft until saved, and
 * every saved change is written to the audit log.
 */
@Composable
fun SystemSettingsScreen(
    state: SystemSettingsUiState,
    onChange: ((SystemSettings) -> SystemSettings) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val draft = state.draft
    val wide = LocalWindowLayout.current.isExpanded
    ActionFeedbackHost(state.message, onMessageShown, modifier) {
        ScreenScaffold {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                    Column(Modifier.weight(1f)) { Security(draft, onChange) }
                    Column(Modifier.weight(1f)) { SiteRules(draft, onChange) }
                }
            } else {
                Security(draft, onChange)
                Spacer(Modifier.height(Spacing.sectionGap))
                SiteRules(draft, onChange)
            }
            Spacer(Modifier.height(Spacing.sectionGap))
            Text(
                text = stringResource(if (state.hasChanges) R.string.system_unsaved else R.string.system_audit_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = stringResource(R.string.system_save),
                onClick = onSave,
                icon = Icons.Filled.Save,
                enabled = state.hasChanges,
                loading = state.isSaving
            )
            if (state.hasChanges) {
                Spacer(Modifier.height(Spacing.itemGap))
                SecondaryButton(text = stringResource(R.string.system_discard), onClick = onDiscard)
            }
        }
    }
}

@Composable
private fun Security(draft: SystemSettings, onChange: ((SystemSettings) -> SystemSettings) -> Unit) {
    SectionHeader(title = stringResource(R.string.system_section_security))
    Spacer(Modifier.height(Spacing.blockGap))
    Text(stringResource(R.string.system_session_timeout), style = MaterialTheme.typography.titleSmall)
    Text(
        stringResource(R.string.system_session_timeout_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.sm))
    ChoiceChipGroup(
        options = SystemSettings.SESSION_TIMEOUT_OPTIONS,
        selected = draft.sessionTimeoutMinutes,
        onSelect = { value -> onChange { it.copy(sessionTimeoutMinutes = value) } },
        label = { pluralStringResource(R.plurals.system_minutes, it, it) }
    )
    Spacer(Modifier.height(Spacing.sectionGap))
    SectionHeader(title = stringResource(R.string.system_section_sync))
    Spacer(Modifier.height(Spacing.blockGap))
    Text(stringResource(R.string.system_sync_interval), style = MaterialTheme.typography.titleSmall)
    Text(
        stringResource(R.string.system_sync_interval_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.sm))
    ChoiceChipGroup(
        options = SystemSettings.SYNC_INTERVAL_OPTIONS,
        selected = draft.syncIntervalMinutes,
        onSelect = { value -> onChange { it.copy(syncIntervalMinutes = value) } },
        label = { pluralStringResource(R.plurals.system_minutes, it, it) }
    )
    Spacer(Modifier.height(Spacing.md))
    SwitchRow(
        title = stringResource(R.string.system_offline_mode),
        subtitle = stringResource(R.string.system_offline_mode_hint),
        checked = draft.allowOfflineMode,
        onCheckedChange = { value -> onChange { it.copy(allowOfflineMode = value) } },
        icon = Icons.Filled.CloudOff
    )
}

@Composable
private fun SiteRules(draft: SystemSettings, onChange: ((SystemSettings) -> SystemSettings) -> Unit) {
    SectionHeader(title = stringResource(R.string.system_section_site))
    Spacer(Modifier.height(Spacing.blockGap))
    SwitchRow(
        title = stringResource(R.string.system_daily_checklist),
        subtitle = stringResource(R.string.system_daily_checklist_hint),
        checked = draft.requireDailyChecklist,
        onCheckedChange = { value -> onChange { it.copy(requireDailyChecklist = value) } },
        icon = Icons.Filled.HealthAndSafety,
        tone = Tone.Warning
    )
    Spacer(Modifier.height(Spacing.sm))
    SwitchRow(
        title = stringResource(R.string.system_low_stock),
        subtitle = stringResource(R.string.system_low_stock_hint),
        checked = draft.lowStockAlerts,
        onCheckedChange = { value -> onChange { it.copy(lowStockAlerts = value) } },
        icon = Icons.Filled.Inventory2,
        tone = Tone.Danger
    )
}

@DevicePreviews
@Composable
private fun SystemSettingsPreview() {
    PreviewSurface {
        SystemSettingsScreen(
            SystemSettingsUiState(draft = SystemSettings(sessionTimeoutMinutes = 60)),
            onChange = {}, onSave = {}, onDiscard = {}, onMessageShown = {}
        )
    }
}
