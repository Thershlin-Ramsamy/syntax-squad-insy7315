package com.mcc.construction.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mcc.construction.R
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone
import java.time.LocalDate

/** A task and the name of its project, ready for a list row. */
data class TaskListItem(
    val task: Task,
    val projectName: String
)

/**
 * A task in a list: title, project, when it is due, and its status as a large
 * chip that reads at arm's length. The whole card is the touch target.
 * [selected] outlines the card when it is the one open in a side-by-side
 * detail pane.
 */
@Composable
fun TaskCard(
    item: TaskListItem,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val task = item.task
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.rowHeight)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface, shape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape)
                }
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.cardPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Spacing.xs))
            MetaLine(Icons.Filled.Work, item.projectName)
            Spacer(Modifier.height(Spacing.xxs))
            val due = deadlineText(task.deadline, today, task.status)
            MetaLine(
                icon = Icons.Filled.Event,
                text = due,
                tone = if (isOverdue(task, today)) Tone.Danger else null
            )
            if (task.priority == TaskPriority.HIGH && task.status != TaskStatus.COMPLETED) {
                Spacer(Modifier.height(Spacing.xxs))
                MetaLine(Icons.Filled.PriorityHigh, stringResource(task.priority.labelRes), Tone.Danger)
            }
        }
        Spacer(Modifier.width(Spacing.md))
        StatusChip(text = stringResource(task.status.labelRes), tone = task.status.tone)
    }
}

@Composable
private fun MetaLine(icon: ImageVector, text: String, tone: Tone? = null) {
    val color = tone?.colors()?.ink ?: MaterialTheme.colorScheme.onSurfaceVariant
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

/** Open work whose deadline has passed. */
fun isOverdue(task: Task, today: LocalDate): Boolean =
    task.status != TaskStatus.COMPLETED && task.deadline.isBefore(today)

/** "Due today", "Due tomorrow", "Overdue since 22 Sep 2026" or "Due 26 Sep 2026". */
@Composable
fun deadlineText(deadline: LocalDate, today: LocalDate, status: TaskStatus): String = when {
    status != TaskStatus.COMPLETED && deadline.isBefore(today) ->
        stringResource(R.string.task_overdue_since, deadline.formatDate())
    deadline == today -> stringResource(R.string.task_due_today)
    deadline == today.plusDays(1) -> stringResource(R.string.task_due_tomorrow)
    else -> stringResource(R.string.task_due_on, deadline.formatDate())
}

/** Order for task lists: work under way first, then what is next, finished work last. */
val TaskWorkOrder: Comparator<Task> = compareBy<Task> {
    when (it.status) {
        TaskStatus.IN_PROGRESS -> 0
        TaskStatus.PENDING -> 1
        TaskStatus.BLOCKED -> 2
        TaskStatus.COMPLETED -> 3
    }
}.thenBy { it.deadline }.thenByDescending { it.priority }

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun TaskCardPreview() {
    val today = LocalDate.now()
    PreviewSurface {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
            TaskCard(
                TaskListItem(
                    Task(101, 1, "Pour concrete foundation — Unit 4", "", today, TaskPriority.HIGH, TaskStatus.IN_PROGRESS),
                    "Riverside Estate Phase 2"
                ),
                today = today,
                onClick = {}
            )
            TaskCard(
                TaskListItem(
                    Task(102, 1, "Erect scaffolding — east elevation", "", today.minusDays(1), TaskPriority.MEDIUM, TaskStatus.PENDING),
                    "Riverside Estate Phase 2"
                ),
                today = today,
                onClick = {},
                selected = true
            )
        }
    }
}
