package com.mcc.construction.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.data.TaskStatus
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.DetailScaffold
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone

@Composable
fun TaskDetailScreen(navController: NavHostController, taskId: String) {
    val task = MockRepository.getTask(taskId)

    DetailScaffold(
        title = "Task details",
        onBack = { navController.popBackStack() },
        bottomBar = {
            if (task != null && task.status != TaskStatus.COMPLETED) {
                // Sticky rather than inline: the primary action stays reachable
                // without scrolling past a long description and safety note.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            horizontal = Spacing.screenH,
                            vertical = Spacing.md
                        )
                ) {
                    PrimaryButton(
                        text = "Mark as complete",
                        icon = Icons.Filled.CheckCircle,
                        onClick = {
                            MockRepository.markComplete(task.id)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    ) {
        if (task == null) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "Task not found",
                message = "This task may have been reassigned or removed.",
                actionLabel = "Go back",
                onAction = { navController.popBackStack() }
            )
            return@DetailScaffold
        }

        ScreenPadding {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.height(Spacing.md))
            }
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusPill(task.status.label, task.status.tone, showDot = true)
                StatusPill("${task.priority.label} priority", task.priority.tone)
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(Icons.Filled.Business, "Project", task.projectName)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Place, "Location", task.location)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Schedule, "Due", task.dueTime)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Person, "Assigned by", task.assignedBy)
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Description")
            Spacer(Modifier.height(Spacing.blockGap))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            task.safetyNote?.let { note ->
                Spacer(Modifier.height(Spacing.sectionGap))
                Banner(
                    title = "Safety note",
                    message = note,
                    icon = Icons.Filled.HealthAndSafety,
                    tone = Tone.Warning
                )
            }

            if (task.status == TaskStatus.COMPLETED) {
                Spacer(Modifier.height(Spacing.sectionGap))
                Banner(
                    title = "Task complete",
                    message = "Marked complete and included in today's report.",
                    icon = Icons.Filled.CheckCircle,
                    tone = Tone.Success
                )
            }
        }
    }
}
