package com.mcc.construction.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.DetailScaffold
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.tone

@Composable
fun ProjectDetailScreen(navController: NavHostController, projectId: String) {
    val project = MockRepository.getProject(projectId)

    DetailScaffold(
        title = "Project details",
        onBack = { navController.popBackStack() }
    ) {
        if (project == null) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "Project not found",
                message = "This project may have been archived or handed over.",
                actionLabel = "Go back",
                onAction = { navController.popBackStack() }
            )
            return@DetailScaffold
        }

        // Tasks are filtered by project name rather than id: the mock task model
        // carries the display name, which is what a real API would join on here.
        val projectTasks = MockRepository.tasks.filter { it.projectName == project.name }

        ScreenPadding {
            Text(
                text = project.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            StatusPill(project.status.label, project.status.tone, showDot = true)

            Spacer(Modifier.height(Spacing.xl))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(project.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                ProgressTrack(project.progress, tone = project.status.tone)
                Spacer(Modifier.height(Spacing.lg))
                KeyValueRow("Budget", project.budget)
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Spent to date", project.spent)
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Open tasks", project.openTasks.toString())
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Team size", "${project.teamSize} people")
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(Icons.Filled.Business, "Client", project.client)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Place, "Location", project.location)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Person, "Site manager", project.manager)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(
                    Icons.Filled.CalendarToday,
                    "Schedule",
                    "${project.startDate} – ${project.targetDate}"
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Scope")
            Spacer(Modifier.height(Spacing.blockGap))
            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(
                title = "Tasks on this project",
                actionLabel = if (projectTasks.isEmpty()) null else "View all",
                onAction = { navController.navigate(Routes.TASKS) }
            )
            Spacer(Modifier.height(Spacing.blockGap))

            if (projectTasks.isEmpty()) {
                Text(
                    text = "No tasks are currently logged against this project.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                projectTasks.forEachIndexed { index, task ->
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
        }
    }
}
