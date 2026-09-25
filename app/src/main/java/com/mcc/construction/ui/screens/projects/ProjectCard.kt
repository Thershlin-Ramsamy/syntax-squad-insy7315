package com.mcc.construction.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mcc.construction.R
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.components.selectedOutline
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** A project with the task counts every project list shows beside it. */
data class ProjectSummary(
    val project: Project,
    val openTasks: Int,
    val blockedTasks: Int,
    val totalTasks: Int
) {
    companion object {
        fun of(project: Project, tasks: List<Task>): ProjectSummary {
            val own = tasks.filter { it.projectId == project.projectId }
            return ProjectSummary(
                project = project,
                openTasks = own.count { it.status != TaskStatus.COMPLETED },
                blockedTasks = own.count { it.status == TaskStatus.BLOCKED },
                totalTasks = own.size
            )
        }
    }
}

/** How close a project is to its end date, for the deadline line on its card. */
fun Project.deadlineTone(today: LocalDate): Tone = when {
    status == ProjectStatus.COMPLETED -> Tone.Success
    endDate.isBefore(today) -> Tone.Danger
    ChronoUnit.DAYS.between(today, endDate) <= DEADLINE_WARNING_DAYS -> Tone.Warning
    else -> Tone.Neutral
}

/** The project's progress as a whole percentage, e.g. "68%". */
val Project.progressLabel: Int get() = progressPercent.roundToInt()

/**
 * A project summary card: name, status, location, deadline and a progress
 * bar. Shared by the manager dashboard and the projects list so the two never
 * drift apart. [selected] outlines it when it is open in the side pane.
 */
@Composable
fun ProjectCard(
    summary: ProjectSummary,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val project = summary.project
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .selectedOutline(selected),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = project.projectName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Spacing.sm))
            StatusChip(stringResource(project.status.labelRes), project.status.tone)
        }
        Spacer(Modifier.height(Spacing.sm))
        MetaLine(Icons.Filled.Place, project.location)
        Spacer(Modifier.height(Spacing.xxs))
        MetaLine(
            icon = Icons.Filled.Event,
            text = stringResource(R.string.project_due, project.endDate.formatDate()),
            tone = project.deadlineTone(today)
        )
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = pluralStringResource(R.plurals.project_open_tasks, summary.openTasks, summary.openTasks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.project_progress_percent, project.progressLabel),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        ProgressTrack(progress = project.progressFraction, tone = project.status.tone)
    }
}

@Composable
internal fun MetaLine(icon: ImageVector, text: String, tone: Tone = Tone.Neutral) {
    val color = if (tone == Tone.Neutral) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        tone.colors().ink
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private const val DEADLINE_WARNING_DAYS = 30
