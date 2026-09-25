package com.mcc.construction.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.AuditLog
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.MetricTile
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.format.relativeTime
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone
import java.time.LocalDateTime

/** Where the administration dashboard can go. */
enum class AdminDestination { USERS, AUDIT_LOG, SYSTEM_SETTINGS, SYNC, EMPLOYEES }

@Composable
fun AdminDashboardRoute(
    onNavigate: (AdminDestination) -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(factory = AdminDashboardViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdminDashboardScreen(state, onNavigate)
}

/**
 * Accounts by role, suspended accounts, the latest audited changes, sync
 * health and the settings that matter most. Two columns on a wide window.
 */
@Composable
fun AdminDashboardScreen(
    state: AdminDashboardUiState,
    onNavigate: (AdminDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }
    val wide = LocalWindowLayout.current.isExpanded
    ScreenScaffold(modifier = modifier, maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth) {
        Text(
            text = stringResource(R.string.admin_greeting, state.firstName),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = stringResource(R.string.admin_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sectionGap))
        Metrics(state, onNavigate)
        Spacer(Modifier.height(Spacing.sectionGap))
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                Column(Modifier.weight(1f)) {
                    Roles(state, onNavigate)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Suspended(state, onNavigate)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Settings(state, onNavigate)
                }
                Column(Modifier.weight(1f)) {
                    Activity(state, onNavigate)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Shortcuts(onNavigate)
                }
            }
        } else {
            Activity(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            Roles(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            Suspended(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            Settings(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            Shortcuts(onNavigate)
        }
    }
}

@Composable
private fun Metrics(state: AdminDashboardUiState, onNavigate: (AdminDestination) -> Unit) {
    val sync = state.sync
    Row(
        modifier = Modifier.height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        MetricTile(
            value = state.activeUsers.toString(),
            label = stringResource(R.string.admin_metric_active),
            icon = Icons.Filled.Groups,
            tone = Tone.Info,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = { onNavigate(AdminDestination.USERS) }
        )
        MetricTile(
            value = state.suspended.size.toString(),
            label = stringResource(R.string.admin_metric_suspended),
            icon = Icons.Filled.Block,
            tone = if (state.suspended.isEmpty()) Tone.Success else Tone.Warning,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = { onNavigate(AdminDestination.USERS) }
        )
        if (LocalWindowLayout.current.isExpanded) {
            MetricTile(
                value = state.totalAuditEntries.toString(),
                label = stringResource(R.string.admin_metric_audit),
                icon = Icons.Filled.History,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onNavigate(AdminDestination.AUDIT_LOG) }
            )
            MetricTile(
                value = (sync?.pendingCount ?: 0).toString(),
                label = stringResource(R.string.admin_metric_waiting),
                icon = sync?.state?.icon ?: Icons.Filled.CloudSync,
                tone = sync?.state?.tone ?: Tone.Neutral,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onNavigate(AdminDestination.SYNC) }
            )
        }
    }
}

@Composable
private fun Roles(state: AdminDashboardUiState, onNavigate: (AdminDestination) -> Unit) {
    SectionHeader(
        title = stringResource(R.string.admin_section_roles),
        actionLabel = stringResource(R.string.admin_manage),
        onAction = { onNavigate(AdminDestination.USERS) }
    )
    Spacer(Modifier.height(Spacing.blockGap))
    AppCard(Modifier.fillMaxWidth()) {
        val total = state.activeUsers.coerceAtLeast(1)
        UserRole.entries.forEachIndexed { index, role ->
            if (index > 0) Spacer(Modifier.height(Spacing.md))
            val count = state.activeByRole[role] ?: 0
            KeyValueRow(stringResource(role.labelRes), count.toString())
            Spacer(Modifier.height(Spacing.xs))
            ProgressTrack(progress = count / total.toFloat(), tone = Tone.Info)
        }
    }
}

@Composable
private fun Suspended(state: AdminDashboardUiState, onNavigate: (AdminDestination) -> Unit) {
    SectionHeader(title = stringResource(R.string.admin_section_suspended))
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.suspended.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.ManageAccounts,
            title = stringResource(R.string.admin_no_suspended_title),
            message = stringResource(R.string.admin_no_suspended_message),
            tone = Tone.Success
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.suspended.forEach { user ->
            ListRow(
                title = user.fullName,
                subtitle = user.email,
                onClick = { onNavigate(AdminDestination.USERS) },
                leading = { Avatar(user.fullName) },
                trailing = { StatusPill(stringResource(R.string.employee_suspended), Tone.Danger, showDot = true) }
            )
        }
    }
}

@Composable
private fun Activity(state: AdminDashboardUiState, onNavigate: (AdminDestination) -> Unit) {
    SectionHeader(
        title = stringResource(R.string.admin_section_activity),
        actionLabel = stringResource(R.string.dashboard_view_all),
        onAction = { onNavigate(AdminDestination.AUDIT_LOG) }
    )
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.recentActivity.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.History,
            title = stringResource(R.string.audit_empty_title),
            message = stringResource(R.string.audit_empty_message)
        )
        return
    }
    AppCard(Modifier.fillMaxWidth()) {
        state.recentActivity.forEachIndexed { index, entry ->
            if (index > 0) Spacer(Modifier.height(Spacing.md))
            AuditEntryRow(entry, state.now)
        }
    }
}

@Composable
private fun Settings(state: AdminDashboardUiState, onNavigate: (AdminDestination) -> Unit) {
    val s = state.settings
    SectionHeader(
        title = stringResource(R.string.admin_section_settings),
        actionLabel = stringResource(R.string.admin_change),
        onAction = { onNavigate(AdminDestination.SYSTEM_SETTINGS) }
    )
    Spacer(Modifier.height(Spacing.blockGap))
    AppCard(Modifier.fillMaxWidth(), onClick = { onNavigate(AdminDestination.SYSTEM_SETTINGS) }) {
        KeyValueRow(stringResource(R.string.system_session_timeout), pluralStringResource(R.plurals.system_minutes, s.sessionTimeoutMinutes, s.sessionTimeoutMinutes))
        Spacer(Modifier.height(Spacing.sm))
        KeyValueRow(stringResource(R.string.system_sync_interval), pluralStringResource(R.plurals.system_minutes, s.syncIntervalMinutes, s.syncIntervalMinutes))
        Spacer(Modifier.height(Spacing.sm))
        KeyValueRow(stringResource(R.string.system_daily_checklist), stringResource(if (s.requireDailyChecklist) R.string.system_on else R.string.system_off))
        Spacer(Modifier.height(Spacing.sm))
        KeyValueRow(stringResource(R.string.system_offline_mode), stringResource(if (s.allowOfflineMode) R.string.system_on else R.string.system_off))
    }
}

@Composable
private fun Shortcuts(onNavigate: (AdminDestination) -> Unit) {
    SectionHeader(title = stringResource(R.string.admin_section_shortcuts))
    Spacer(Modifier.height(Spacing.blockGap))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Shortcut(Icons.Filled.ManageAccounts, R.string.title_user_management, R.string.admin_users_hint) { onNavigate(AdminDestination.USERS) }
        Shortcut(Icons.AutoMirrored.Filled.FactCheck, R.string.title_audit_log, R.string.admin_audit_hint) { onNavigate(AdminDestination.AUDIT_LOG) }
        Shortcut(Icons.Filled.Tune, R.string.title_system_settings, R.string.admin_settings_hint) { onNavigate(AdminDestination.SYSTEM_SETTINGS) }
        Shortcut(Icons.Filled.CloudSync, R.string.title_sync_status, R.string.admin_sync_hint) { onNavigate(AdminDestination.SYNC) }
    }
}

@Composable
private fun Shortcut(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, subtitle: Int, onClick: () -> Unit) {
    ListRow(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        onClick = onClick,
        leading = { LeadingIcon(icon) },
        trailing = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}

private val WIDE_CONTENT: Dp = 1200.dp

internal fun previewAudit(now: LocalDateTime): List<AuditEntry> = listOf(
    AuditEntry(AuditLog(1, 1, "Changed role of Kagiso Molefe to Worker", now.minusMinutes(35)), "Riaan van Wyk"),
    AuditEntry(AuditLog(2, 2, "Approved daily report #2 — Riverside Estate Phase 2", now.minusDays(1)), "Naledi Khumalo"),
    AuditEntry(AuditLog(3, 1, "Suspended account pieter.coetzee@mcc.co.za", now.minusDays(2)), "Riaan van Wyk")
)

@DevicePreviews
@Composable
private fun AdminDashboardPreview() {
    val now = LocalDateTime.now()
    PreviewSurface {
        AdminDashboardScreen(
            AdminDashboardUiState(
                isLoading = false,
                firstName = "Riaan",
                activeByRole = mapOf(UserRole.WORKER to 6, UserRole.MANAGER to 3, UserRole.ADMINISTRATOR to 1),
                suspended = listOf(User(9, UserRole.MANAGER, "Pieter", "Coetzee", "pieter.coetzee@mcc.co.za", "", "Quantity Surveyor", "MCC-1020", false)),
                recentActivity = previewAudit(now),
                totalAuditEntries = 5,
                sync = SyncStatus(true, false, false, emptyList(), now),
                now = now
            ),
            onNavigate = {}
        )
    }
}
