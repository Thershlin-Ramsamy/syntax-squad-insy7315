package com.mcc.construction.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.ChoiceChip
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.screens.employees.AttendanceToday
import com.mcc.construction.ui.screens.employees.PresenceStatus
import com.mcc.construction.ui.screens.employees.labelRes
import com.mcc.construction.ui.screens.employees.tone
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Callbacks for the Assign Task form. */
data class AssignTaskActions(
    val onBack: () -> Unit = {},
    val onProjectSelected: (Int) -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onDeadlineSelected: (LocalDate) -> Unit = {},
    val onPrioritySelected: (TaskPriority) -> Unit = {},
    val onToggleWorker: (Int) -> Unit = {},
    val onSave: () -> Unit = {},
    val onAssignAnother: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

@Composable
fun AssignTaskRoute(
    projectId: Int,
    onBack: () -> Unit,
    viewModel: AssignTaskViewModel = viewModel(
        key = "assign-task-$projectId",
        factory = AssignTaskViewModel.factory(projectId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AssignTaskScreen(
        state = state,
        actions = AssignTaskActions(
            onBack = onBack,
            onProjectSelected = viewModel::onProjectSelected,
            onTitleChange = viewModel::onTitleChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onDeadlineSelected = viewModel::onDeadlineSelected,
            onPrioritySelected = viewModel::onPrioritySelected,
            onToggleWorker = viewModel::onToggleWorker,
            onSave = viewModel::onSave,
            onAssignAnother = viewModel::onAssignAnother,
            onMessageShown = viewModel::onMessageShown
        )
    )
}

/**
 * Create a task and hand it to one or more workers. On a wide window the task
 * details and the crew sit side by side; on a phone they stack, with the save
 * button pinned to the bottom.
 */
@Composable
fun AssignTaskScreen(
    state: AssignTaskUiState,
    actions: AssignTaskActions,
    modifier: Modifier = Modifier
) {
    val wide = LocalWindowLayout.current.isExpanded
    val saved = state.form.saved
    ActionFeedbackHost(state.message, actions.onMessageShown, modifier) {
        ScreenScaffold(
            title = stringResource(R.string.assign_title),
            onBack = actions.onBack,
            maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth
        ) {
            when {
                state.isLoading -> LoadingState()
                saved != null -> Column(Modifier.widthIn(max = SAVED_WIDTH)) { Saved(saved, state, actions) }
                wide -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                        Column(Modifier.weight(1f)) { TaskFields(state, actions) }
                        Column(Modifier.weight(1f)) {
                            Workers(state, actions)
                            Spacer(Modifier.height(Spacing.sectionGap))
                            SaveButton(state, actions)
                        }
                    }
                }
                else -> {
                    TaskFields(state, actions)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Workers(state, actions)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    SaveButton(state, actions)
                }
            }
        }
    }
}

@Composable
private fun TaskFields(state: AssignTaskUiState, actions: AssignTaskActions) {
    val form = state.form
    SectionHeader(title = stringResource(R.string.assign_section_project))
    Spacer(Modifier.height(Spacing.blockGap))
    ChoiceChipGroup(
        options = state.projects.map { it.projectId },
        selected = form.projectId,
        onSelect = actions.onProjectSelected,
        label = { id -> state.projects.first { it.projectId == id }.projectName }
    )

    Spacer(Modifier.height(Spacing.sectionGap))
    SectionHeader(title = stringResource(R.string.assign_section_task))
    Spacer(Modifier.height(Spacing.blockGap))
    AppTextField(
        value = form.title,
        onValueChange = actions.onTitleChange,
        label = stringResource(R.string.assign_field_title),
        placeholder = stringResource(R.string.assign_field_title_hint),
        isError = AssignField.TITLE in form.missing,
        supportingText = if (AssignField.TITLE in form.missing) stringResource(R.string.assign_title_required) else null,
        imeAction = ImeAction.Next,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.itemGap))
    AppTextField(
        value = form.description,
        onValueChange = actions.onDescriptionChange,
        label = stringResource(R.string.assign_field_description),
        placeholder = stringResource(R.string.assign_field_description_hint),
        singleLine = false,
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(Spacing.sectionGap))
    SectionHeader(title = stringResource(R.string.assign_section_deadline))
    Spacer(Modifier.height(Spacing.blockGap))
    DeadlinePicker(state, actions)

    Spacer(Modifier.height(Spacing.sectionGap))
    SectionHeader(title = stringResource(R.string.assign_section_priority))
    Spacer(Modifier.height(Spacing.blockGap))
    ChoiceChipGroup(
        options = TaskPriority.entries.toList(),
        selected = form.priority,
        onSelect = actions.onPrioritySelected,
        label = { stringResource(it.labelRes) },
        icon = { Icons.Filled.Flag }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DeadlinePicker(state: AssignTaskUiState, actions: AssignTaskActions) {
    val today = state.today
    val quick = listOf(
        today to R.string.assign_deadline_today,
        today.plusDays(1) to R.string.assign_deadline_tomorrow,
        today.plusDays(3) to R.string.assign_deadline_three_days,
        today.plusWeeks(1) to R.string.assign_deadline_next_week
    )
    val picked = state.form.deadline
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val isCustom = picked != null && quick.none { it.first == picked }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        quick.forEach { (date, label) ->
            ChoiceChip(label = stringResource(label), selected = picked == date, onClick = { actions.onDeadlineSelected(date) })
        }
        ChoiceChip(
            label = if (isCustom) picked.formatDate() else stringResource(R.string.assign_deadline_pick),
            selected = isCustom,
            onClick = { showPicker = true },
            icon = Icons.Filled.CalendarMonth
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    val missing = AssignField.DEADLINE in state.form.missing
    Text(
        text = when {
            missing -> stringResource(R.string.assign_deadline_required)
            picked != null -> stringResource(R.string.assign_deadline_chosen, picked.formatDate())
            else -> stringResource(R.string.assign_deadline_none)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (missing) Tone.Danger.colors().ink else MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (showPicker) {
        val todayMillis = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (picked ?: today).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = remember(todayMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        actions.onDeadlineSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun Workers(state: AssignTaskUiState, actions: AssignTaskActions) {
    val chosen = state.form.workerIds.size
    SectionHeader(title = pluralStringResource(R.plurals.assign_section_workers, chosen, chosen))
    Spacer(Modifier.height(Spacing.xs))
    val missing = AssignField.WORKERS in state.form.missing
    Text(
        text = stringResource(if (missing) R.string.assign_workers_required else R.string.assign_workers_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = if (missing) Tone.Danger.colors().ink else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.blockGap))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.workers.forEach { option ->
            WorkerRow(option, selected = option.user.userId in state.form.workerIds) {
                actions.onToggleWorker(option.user.userId)
            }
        }
    }
}

@Composable
private fun WorkerRow(option: WorkerOption, selected: Boolean, onToggle: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.rowHeight)
            .background(
                if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                shape
            )
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.size(Spacing.touchTarget))
        Spacer(Modifier.width(Spacing.xs))
        Avatar(option.user.fullName)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(option.user.fullName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${option.user.jobTitle} · " +
                    pluralStringResource(R.plurals.project_member_tasks, option.openTasks, option.openTasks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        StatusPill(stringResource(option.today.status.labelRes), option.today.status.tone, showDot = true)
    }
}

@Composable
private fun SaveButton(state: AssignTaskUiState, actions: AssignTaskActions) {
    val count = state.form.workerIds.size
    PrimaryButton(
        text = if (count == 0) {
            stringResource(R.string.assign_save)
        } else {
            pluralStringResource(R.plurals.assign_save_to, count, count)
        },
        onClick = actions.onSave,
        icon = Icons.Filled.PersonAdd,
        loading = state.form.isSaving
    )
}

@Composable
private fun Saved(result: AssignResult, state: AssignTaskUiState, actions: AssignTaskActions) {
    AppCard(Modifier.fillMaxWidth()) {
        val ink = Tone.Success.colors().ink
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(Spacing.md))
        Text(stringResource(R.string.assign_saved_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.assign_saved_message, result.task.title, joinNames(result.workerNames)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(
                R.string.assign_saved_details,
                state.projects.firstOrNull { it.projectId == result.task.projectId }?.projectName.orEmpty(),
                result.task.deadline.formatDate()
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(Spacing.sectionGap))
    PrimaryButton(text = stringResource(R.string.assign_done), onClick = actions.onBack)
    Spacer(Modifier.height(Spacing.itemGap))
    SecondaryButton(text = stringResource(R.string.assign_another), onClick = actions.onAssignAnother, icon = Icons.Filled.PersonAdd)
}

/** "Sipho", "Sipho and Ayanda", "Sipho, Thabo and Ayanda". */
@Composable
private fun joinNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.first()
    else -> stringResource(R.string.names_and, names.dropLast(1).joinToString(", "), names.last())
}

private val WIDE_CONTENT: Dp = 1120.dp
private val SAVED_WIDTH: Dp = 640.dp

private fun previewState(saved: Boolean = false): AssignTaskUiState {
    val today = LocalDate.now()
    fun worker(id: Int, first: String, last: String, job: String) =
        User(id, UserRole.WORKER, first, last, "$first@mcc.co.za", "", job, "MCC-$id")
    val project = Project(1, 2, "Riverside Estate Phase 2", "Centurion", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0)
    val task = Task(200, 1, "Strip formwork on Block C", "", today.plusDays(1), TaskPriority.HIGH, TaskStatus.PENDING)
    return AssignTaskUiState(
        isLoading = false,
        projects = listOf(project, project.copy(projectId = 2, projectName = "Harbour View Offices")),
        workers = listOf(
            WorkerOption(worker(5, "Sipho", "Dlamini", "Site Operative"), AttendanceToday(PresenceStatus.ON_SITE), 3),
            WorkerOption(worker(6, "Thabo", "Mokoena", "Bricklayer"), AttendanceToday(PresenceStatus.ON_SITE), 2),
            WorkerOption(worker(8, "Anele", "Mthembu", "Electrician"), AttendanceToday.NotIn, 1)
        ),
        form = AssignTaskForm(
            projectId = 1,
            title = "Strip formwork on Block C",
            deadline = today.plusDays(1),
            priority = TaskPriority.HIGH,
            workerIds = setOf(5),
            saved = if (saved) AssignResult(task, listOf("Sipho", "Thabo")) else null
        ),
        today = today
    )
}

@DevicePreviews
@Composable
private fun AssignTaskPreview() {
    PreviewSurface { AssignTaskScreen(previewState(), AssignTaskActions()) }
}

@DevicePreviews
@Composable
private fun AssignTaskSavedPreview() {
    PreviewSurface { AssignTaskScreen(previewState(saved = true), AssignTaskActions()) }
}
