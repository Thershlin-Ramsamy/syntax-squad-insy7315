package com.mcc.construction.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.AccountStatus
import com.mcc.construction.data.AuditSeverity
import com.mcc.construction.data.MockRepository
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.MetricTile
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.label
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone

/**
 * Administrator home.
 *
 * An administrator is watching the system rather than the site, so this leads
 * with account health and anything security-relevant in the audit log — a
 * critical entry is a banner, not a row someone has to go looking for.
 */
@Composable
fun AdminDashboardScreen(navController: NavHostController) {
    val users = MockRepository.managedUsers
    val audit = MockRepository.auditLog

    val active = users.count { it.status == AccountStatus.ACTIVE }
    val suspended = users.count { it.status == AccountStatus.SUSPENDED }
    val invited = users.count { it.status == AccountStatus.INVITED }
    val critical = audit.filter { it.severity == AuditSeverity.CRITICAL }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            Text(
                text = "System overview",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${users.size} accounts · ${audit.size} logged events",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.xl))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
                MetricTile(
                    value = active.toString(),
                    label = "Active users",
                    icon = Icons.Filled.Groups,
                    tone = Tone.Success,
                    onClick = { navController.navigate(Routes.USER_MANAGEMENT) },
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = suspended.toString(),
                    label = "Suspended",
                    icon = Icons.Filled.Block,
                    tone = if (suspended == 0) Tone.Neutral else Tone.Danger,
                    onClick = { navController.navigate(Routes.USER_MANAGEMENT) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (critical.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                Banner(
                    title = "Security event needs review",
                    message = "${critical.first().action} — ${critical.first().target} " +
                        "(${critical.first().timestamp})",
                    icon = Icons.Filled.WarningAmber,
                    tone = Tone.Danger
                )
            }

            if (invited > 0) {
                Spacer(Modifier.height(Spacing.itemGap))
                Banner(
                    title = "$invited invitation${if (invited == 1) "" else "s"} outstanding",
                    message = "These accounts have not signed in yet.",
                    icon = Icons.Filled.ManageAccounts,
                    tone = Tone.Warning
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Accounts by role")
            Spacer(Modifier.height(Spacing.blockGap))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                val byRole = users.groupBy { it.role }
                byRole.entries.forEachIndexed { index, (role, list) ->
                    if (index > 0) Spacer(Modifier.height(Spacing.md))
                    KeyValueRow(role.label, list.size.toString())
                }
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(
                title = "Recent activity",
                actionLabel = "View log",
                onAction = { navController.navigate(Routes.AUDIT_LOG) }
            )
            Spacer(Modifier.height(Spacing.blockGap))

            audit.take(3).forEachIndexed { index, entry ->
                if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                ListRow(
                    title = entry.action,
                    subtitle = "${entry.actor} · ${entry.target}",
                    meta = entry.timestamp,
                    onClick = { navController.navigate(Routes.AUDIT_LOG) },
                    leading = { LeadingIcon(Icons.Filled.Gavel, entry.severity.tone) },
                    trailing = { StatusPill(entry.severity.label, entry.severity.tone) }
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Administration")
            Spacer(Modifier.height(Spacing.blockGap))

            ListRow(
                title = "User management",
                subtitle = "Roles, suspensions and invitations",
                onClick = { navController.navigate(Routes.USER_MANAGEMENT) },
                leading = { LeadingIcon(Icons.Filled.ManageAccounts, Tone.Info) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Audit log",
                subtitle = "${audit.size} events recorded",
                onClick = { navController.navigate(Routes.AUDIT_LOG) },
                leading = { LeadingIcon(Icons.AutoMirrored.Filled.FactCheck, Tone.Neutral) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Sync status",
                subtitle = "Device upload queue",
                onClick = { navController.navigate(Routes.SYNC_STATUS) },
                leading = { LeadingIcon(Icons.Filled.CloudSync, Tone.Neutral) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "System settings",
                subtitle = "Appearance, alerts, data usage",
                onClick = { navController.navigate(Routes.SETTINGS) },
                leading = { LeadingIcon(Icons.Filled.Settings, Tone.Neutral) }
            )
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}
