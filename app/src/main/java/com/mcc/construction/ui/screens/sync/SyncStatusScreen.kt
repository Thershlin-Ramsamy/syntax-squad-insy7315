package com.mcc.construction.ui.screens.sync

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.DetailScaffold
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.Eyebrow
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

/**
 * Offline and sync status.
 *
 * CLAUDE.md makes offline-first a visible property of the app, so this screen
 * says three things plainly: whether there is a connection, what is still
 * waiting to upload, and what happens if the worker walks out of coverage.
 * The connection toggle is here so the offline path can be demonstrated on a
 * device that is in fact online.
 */
@Composable
fun SyncStatusScreen(navController: NavHostController) {
    val online by MockRepository.isOnline
    val pending by MockRepository.pendingSyncCount
    val queue = MockRepository.syncQueue

    val tone = when {
        !online -> Tone.Danger
        pending > 0 -> Tone.Warning
        else -> Tone.Success
    }

    DetailScaffold(
        title = "Offline & sync",
        onBack = { navController.popBackStack() }
    ) {
        ScreenPadding {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Eyebrow("Connection")
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = if (online) "Online" else "Offline",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.sm))
                StatusPill(
                    text = when {
                        !online -> "Working from the device"
                        pending > 0 -> "$pending item${if (pending == 1) "" else "s"} queued"
                        else -> "Everything uploaded"
                    },
                    tone = tone,
                    showDot = true
                )
                Spacer(Modifier.height(Spacing.lg))
                KeyValueRow("Last successful sync", if (pending > 0) "Today, 06:41" else "Just now")
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Queued items", pending.toString())
            }

            Spacer(Modifier.height(Spacing.lg))

            if (online) {
                PrimaryButton(
                    text = if (pending > 0) "Sync now" else "Everything is up to date",
                    icon = if (pending > 0) Icons.Filled.CloudSync else Icons.Filled.CloudDone,
                    enabled = pending > 0,
                    onClick = { MockRepository.syncNow() }
                )
            } else {
                Banner(
                    title = "No connection",
                    message = "Work carries on as normal. Reports and checklists are saved " +
                        "on the device and upload automatically when you are back in range.",
                    icon = Icons.Filled.CloudOff,
                    tone = Tone.Danger
                )
            }

            Spacer(Modifier.height(Spacing.itemGap))

            SecondaryButton(
                text = if (online) "Simulate going offline" else "Simulate coming back online",
                icon = if (online) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                onClick = { MockRepository.toggleOnline() }
            )

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Waiting to upload")
            Spacer(Modifier.height(Spacing.blockGap))

            if (pending == 0) {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "Queue is empty",
                    message = "Every report and checklist has reached the site office.",
                    tone = Tone.Success
                )
            } else {
                queue.forEachIndexed { index, item ->
                    if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                    ListRow(
                        title = item.label,
                        subtitle = if (online) "Uploading when you tap Sync now" else item.detail,
                        meta = "Queued ${item.queuedAt}",
                        leading = { LeadingIcon(Icons.Filled.Schedule, Tone.Warning) }
                    )
                }
            }
        }
    }
}
