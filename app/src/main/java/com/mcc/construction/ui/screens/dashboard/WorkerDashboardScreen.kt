package com.mcc.construction.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.data.TaskStatus
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.AccentButton
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.MetricTile
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone

/**
 * Worker home.
 *
 * Ordered by what a worker needs in the first five seconds on site: who they
 * are and where, the two figures that matter, then the single largest control
 * on the screen — clock in. Detail lists come after. CLAUDE.md asks for worker
 * flows in three taps or fewer, and every action here is one.
 */
@Composable
fun WorkerDashboardScreen(navController: NavHostController) {
    val user = MockRepository.currentUser
    val tasks = MockRepository.tasks
    val isClockedIn by MockRepository.isClockedIn
    val checklist = MockRepository.checklist

    val open = tasks.count { it.status != TaskStatus.COMPLETED }
    val done = tasks.count { it.status == TaskStatus.COMPLETED }
    val todays = tasks.filter { it.status != TaskStatus.COMPLETED }.take(3)
    val checklistDone = checklist.count { it.checked }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            Text(
                text = "Good morning, ${user.name.substringBefore(" ")}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = user.site,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.xl))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
                MetricTile(
                    value = open.toString(),
                    label = "Open tasks",
                    icon = Icons.Filled.PendingActions,
                    tone = Tone.Warning,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = done.toString(),
                    label = "Completed",
                    icon = Icons.Filled.CheckCircle,
                    tone = Tone.Success,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            AccentButton(
                text = if (isClockedIn) "Clocked in since 06:52 — tap to clock out" else "Clock In",
                icon = Icons.Filled.AccessTime,
                onClick = { MockRepository.toggleClockedIn() }
            )

            if (checklistDone < checklist.size) {
                Spacer(Modifier.height(Spacing.lg))
                Banner(
                    title = "Safety checklist incomplete",
                    message = "$checklistDone of ${checklist.size} items done. " +
                        "Complete it before starting work.",
                    icon = Icons.Filled.WarningAmber,
                    tone = Tone.Warning
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(
                title = "Today's tasks",
                actionLabel = "View all",
                onAction = { navController.navigate(Routes.TASKS) }
            )
            Spacer(Modifier.height(Spacing.blockGap))

            if (todays.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "Nothing outstanding",
                    message = "Every task assigned to you today is complete.",
                    tone = Tone.Success
                )
            } else {
                todays.forEachIndexed { index, task ->
                    if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                    ListRow(
                        title = task.title,
                        subtitle = task.location,
                        meta = task.dueTime,
                        onClick = { navController.navigate(Routes.taskDetail(task.id)) },
                        trailing = { StatusPill(task.status.label, task.status.tone) }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Quick actions")
            Spacer(Modifier.height(Spacing.blockGap))

            ListRow(
                title = "Safety checklist",
                subtitle = "$checklistDone of ${checklist.size} completed",
                onClick = { navController.navigate(Routes.SAFETY_CHECKLIST) },
                leading = { LeadingIcon(Icons.Filled.HealthAndSafety, Tone.Warning) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Submit daily report",
                subtitle = "Log today's hours and work done",
                onClick = { navController.navigate(Routes.DAILY_REPORT) },
                leading = { LeadingIcon(Icons.Filled.Description, Tone.Info) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Report an incident",
                subtitle = "Near miss, injury or hazard",
                onClick = { navController.navigate(Routes.INCIDENT_REPORT) },
                leading = { LeadingIcon(Icons.Filled.WarningAmber, Tone.Danger) }
            )
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}
