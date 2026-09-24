package com.mcc.construction.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import java.time.LocalDate

/**
 * Task list wired to its ViewModel.
 *
 * On a phone, tapping a task calls [onOpenTask] to push the detail screen. On a
 * wide window the list keeps a detail pane beside it, filled by [detailPane],
 * so a task can be read and updated without leaving the list.
 */
@Composable
fun TaskListRoute(
    onOpenTask: (Int) -> Unit,
    detailPane: (@Composable (taskId: Int) -> Unit)? = null,
    viewModel: TaskListViewModel = viewModel(factory = TaskListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val twoPane = LocalWindowLayout.current.isExpanded && detailPane != null
    TaskListScreen(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onTaskClick = { id -> if (twoPane) viewModel.onSelectTask(id) else onOpenTask(id) },
        detailPane = if (twoPane) detailPane else null
    )
}

/**
 * Tasks with large status chips, filtered by status. Under "All", work due now
 * is listed ahead of what is coming up.
 */
@Composable
fun TaskListScreen(
    state: TaskListUiState,
    onFilterChange: (TaskFilter) -> Unit,
    onTaskClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    detailPane: (@Composable (taskId: Int) -> Unit)? = null
) {
    if (state.items == null) {
        LoadingState(modifier)
        return
    }
    if (detailPane == null) {
        ScreenScaffold(modifier = modifier, scrollable = false) {
            TaskListColumn(state, onFilterChange, onTaskClick, highlightSelection = false)
        }
        return
    }
    ScreenScaffold(modifier = modifier, scrollable = false, maxContentWidth = Dp.Unspecified) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(LIST_WEIGHT)) {
                TaskListColumn(state, onFilterChange, onTaskClick, highlightSelection = true)
            }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Box(Modifier.weight(1f - LIST_WEIGHT)) {
                val paneTask = state.paneTaskId
                if (paneTask != null) {
                    detailPane(paneTask)
                } else {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        title = stringResource(R.string.tasks_pick_title),
                        message = stringResource(R.string.tasks_pick_message)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskListColumn(
    state: TaskListUiState,
    onFilterChange: (TaskFilter) -> Unit,
    onTaskClick: (Int) -> Unit,
    highlightSelection: Boolean
) {
    FilterRow(state, onFilterChange)
    Spacer(Modifier.height(Spacing.blockGap))

    if (state.visible.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.TaskAlt,
            title = stringResource(
                if (state.items.isNullOrEmpty()) R.string.tasks_empty_title else R.string.tasks_filter_empty_title
            ),
            message = stringResource(
                if (state.items.isNullOrEmpty()) R.string.tasks_empty_message else R.string.tasks_filter_empty_message
            ),
            tone = if (state.items.isNullOrEmpty()) Tone.Neutral else Tone.Success
        )
        return
    }

    val selected = if (highlightSelection) state.paneTaskId else null
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.xxxl, end = if (highlightSelection) Spacing.lg else Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        if (state.filter == TaskFilter.ALL) {
            section(R.string.tasks_section_now, state.dueNow, state.today, selected, onTaskClick)
            section(R.string.tasks_section_coming_up, state.comingUp, state.today, selected, onTaskClick)
        } else {
            items(state.visible, key = { it.task.taskId }) { item ->
                TaskCard(item, state.today, onClick = { onTaskClick(item.task.taskId) }, selected = item.task.taskId == selected)
            }
        }
    }
}

private fun LazyListScope.section(
    titleRes: Int,
    items: List<TaskListItem>,
    today: LocalDate,
    selected: Int?,
    onTaskClick: (Int) -> Unit
) {
    if (items.isEmpty()) return
    item(key = "header-$titleRes") {
        SectionHeader(title = stringResource(titleRes))
    }
    items(items, key = { it.task.taskId }) { item ->
        TaskCard(item, today, onClick = { onTaskClick(item.task.taskId) }, selected = item.task.taskId == selected)
    }
}

@Composable
private fun FilterRow(state: TaskListUiState, onFilterChange: (TaskFilter) -> Unit) {
    val labels = TaskFilter.entries.associateWith { filter ->
        stringResource(filter.labelRes, state.countFor(filter))
    }
    if (state.showsAllTasks) {
        Text(
            text = stringResource(R.string.tasks_showing_all),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))
    }
    SegmentedFilter(
        options = TaskFilter.entries.map { labels.getValue(it) },
        selected = labels.getValue(state.filter),
        onSelect = { label -> labels.entries.first { it.value == label }.key.let(onFilterChange) }
    )
}

private val TaskFilter.labelRes: Int
    get() = when (this) {
        TaskFilter.ALL -> R.string.tasks_filter_all
        TaskFilter.PENDING -> R.string.tasks_filter_pending
        TaskFilter.IN_PROGRESS -> R.string.tasks_filter_in_progress
        TaskFilter.COMPLETED -> R.string.tasks_filter_completed
    }

private const val LIST_WEIGHT = 0.45f

private fun previewState(): TaskListUiState {
    val today = LocalDate.now()
    val project = "Riverside Estate Phase 2"
    return TaskListUiState(
        items = listOf(
            TaskListItem(Task(101, 1, "Pour concrete foundation — Unit 4", "", today, TaskPriority.HIGH, TaskStatus.IN_PROGRESS), project),
            TaskListItem(Task(102, 1, "Erect scaffolding — east elevation", "", today, TaskPriority.MEDIUM, TaskStatus.PENDING), project),
            TaskListItem(Task(105, 1, "Electrical conduit rough-in — Unit 2", "", today.plusDays(1), TaskPriority.MEDIUM, TaskStatus.PENDING), project),
            TaskListItem(Task(103, 1, "Morning safety walkaround", "", today, TaskPriority.HIGH, TaskStatus.COMPLETED), project)
        ),
        today = today
    )
}

@DevicePreviews
@Composable
private fun TaskListPreview() {
    PreviewSurface {
        TaskListScreen(state = previewState(), onFilterChange = {}, onTaskClick = {})
    }
}

@DevicePreviews
@Composable
private fun TaskListTwoPanePreview() {
    PreviewSurface {
        TaskListScreen(
            state = previewState(),
            onFilterChange = {},
            onTaskClick = {},
            detailPane = { id -> Text("Task $id", Modifier.width(Spacing.xxxl)) }
        )
    }
}
