package com.mcc.construction.ui.screens.employees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.MetricInline
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.format.formatHours
import com.mcc.construction.ui.format.formatTime
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.screens.tasks.TaskCard
import com.mcc.construction.ui.screens.tasks.TaskListItem
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Employee Profile wired to its ViewModel.
 *
 * @param embedded true in the employees list's side pane: no app bar of its
 *   own and a single column.
 */
@Composable
fun EmployeeProfileRoute(
    userId: Int,
    onBack: () -> Unit,
    onOpenTask: (Int) -> Unit,
    onMessage: (Int) -> Unit,
    embedded: Boolean = false,
    viewModel: EmployeeProfileViewModel = viewModel(
        key = "employee-profile-$userId",
        factory = EmployeeProfileViewModel.factory(userId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EmployeeProfileScreen(state, embedded, onBack, onOpenTask, onMessage)
}

/** Contact details, today's attendance, current tasks and a fortnight of shifts with hours. */
@Composable
fun EmployeeProfileScreen(
    state: EmployeeProfileUiState,
    embedded: Boolean,
    onBack: () -> Unit,
    onOpenTask: (Int) -> Unit,
    onMessage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val user = state.user
    val twoColumns = LocalWindowLayout.current.isExpanded && !embedded
    ScreenScaffold(
        modifier = modifier,
        title = if (embedded) null else user?.fullName ?: stringResource(R.string.employee_profile_title),
        onBack = if (embedded) null else onBack,
        maxContentWidth = if (twoColumns) WIDE_CONTENT else Spacing.contentMaxWidth
    ) {
        when {
            state.isLoading -> LoadingState()
            user == null -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(R.string.employee_not_found_title),
                message = stringResource(R.string.employee_not_found_message)
            )
            twoColumns -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                Column(Modifier.weight(1f)) {
                    Header(state, user, onMessage)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    CurrentTasks(state, onOpenTask)
                }
                Column(Modifier.weight(1f)) { AttendanceHistory(state) }
            }
            else -> {
                Header(state, user, onMessage)
                Spacer(Modifier.height(Spacing.sectionGap))
                CurrentTasks(state, onOpenTask)
                Spacer(Modifier.height(Spacing.sectionGap))
                AttendanceHistory(state)
            }
        }
    }
}

@Composable
private fun Header(state: EmployeeProfileUiState, user: User, onMessage: (Int) -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(user.fullName, size = 64.dp)
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(user.fullName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(user.jobTitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatusChip(stringResource(user.role.labelRes), Tone.Info)
                    if (user.isActive) {
                        StatusPill(stringResource(state.today.status.labelRes), state.today.status.tone, showDot = true)
                    } else {
                        StatusPill(stringResource(R.string.employee_suspended), Tone.Danger, showDot = true)
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Spacing.lg))
        InfoRow(Icons.Filled.Phone, stringResource(R.string.profile_phone), user.phone)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Email, stringResource(R.string.profile_email), user.email)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Badge, stringResource(R.string.profile_employee_number), user.employeeNumber)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.AccessTime, stringResource(R.string.employee_today), state.today.detailText())
    }
    Spacer(Modifier.height(Spacing.lg))
    PrimaryButton(
        text = stringResource(R.string.employee_send_message, user.firstName),
        onClick = { onMessage(user.userId) },
        icon = Icons.AutoMirrored.Filled.Chat
    )
}

@Composable
private fun CurrentTasks(state: EmployeeProfileUiState, onOpenTask: (Int) -> Unit) {
    SectionHeader(title = stringResource(R.string.employee_current_tasks, state.currentTasks.size))
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.currentTasks.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.TaskAlt,
            title = stringResource(R.string.employee_no_tasks_title),
            message = pluralStringResource(R.plurals.employee_no_tasks_message, state.completedTasks, state.completedTasks)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        state.currentTasks.forEach { item ->
            TaskCard(item, state.date, onClick = { onOpenTask(item.task.taskId) })
        }
    }
}

@Composable
private fun AttendanceHistory(state: EmployeeProfileUiState) {
    SectionHeader(title = stringResource(R.string.employee_attendance))
    Spacer(Modifier.height(Spacing.blockGap))
    AppCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            MetricInline(
                stringResource(R.string.attendance_hours_value, formatHours(state.hoursThisWeek)),
                stringResource(R.string.employee_hours_week),
                Modifier.weight(1f)
            )
            MetricInline(
                stringResource(R.string.attendance_hours_value, formatHours(state.hoursLastFortnight)),
                stringResource(R.string.employee_hours_fortnight),
                Modifier.weight(1f)
            )
            MetricInline(
                state.shifts.map { it.date }.distinct().size.toString(),
                stringResource(R.string.employee_days_worked),
                Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(Spacing.itemGap))
    if (state.shifts.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.AccessTime,
            title = stringResource(R.string.employee_no_shifts_title),
            message = stringResource(R.string.employee_no_shifts_message)
        )
        return
    }
    AppCard(Modifier.fillMaxWidth()) {
        state.shifts.forEachIndexed { index, shift ->
            if (index > 0) {
                HorizontalDivider(Modifier.padding(vertical = Spacing.sm), color = MaterialTheme.colorScheme.outlineVariant)
            }
            ShiftRow(shift, state.date)
        }
    }
}

@Composable
private fun ShiftRow(shift: Attendance, today: LocalDate) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (shift.date == today) stringResource(R.string.dashboard_today) else shift.date.format(DAY_FORMAT),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.employee_shift_times,
                    shift.clockIn.formatTime(),
                    shift.clockOut?.formatTime() ?: stringResource(R.string.employee_shift_open)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (shift.isOpen) {
            StatusPill(stringResource(R.string.presence_on_site), Tone.Success, showDot = true)
        } else {
            Text(
                text = stringResource(R.string.attendance_hours_value, formatHours(shift.totalHours ?: 0.0)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val WIDE_CONTENT: Dp = 1120.dp

private fun previewState(): EmployeeProfileUiState {
    val item = previewEmployees().first()
    val today = LocalDate.now()
    val shifts = (0L..5L).map { days ->
        val date = today.minusDays(days)
        val start = date.atTime(7, 0)
        if (days == 0L) Attendance(100, 5, date, start, null, null)
        else Attendance(100 + days.toInt(), 5, date, start, start.plusHours(9), 8.5)
    }
    return EmployeeProfileUiState(
        isLoading = false,
        user = item.user,
        today = item.today,
        currentTasks = listOf(
            TaskListItem(Task(101, 1, "Pour concrete foundation — Unit 4", "", today, TaskPriority.HIGH, TaskStatus.IN_PROGRESS), "Riverside Estate Phase 2"),
            TaskListItem(Task(102, 1, "Erect scaffolding — east elevation", "", today.plusDays(1), TaskPriority.MEDIUM, TaskStatus.PENDING), "Riverside Estate Phase 2")
        ),
        completedTasks = 4,
        shifts = shifts,
        hoursThisWeek = 25.5,
        hoursLastFortnight = 68.0,
        date = today
    )
}

@DevicePreviews
@Composable
private fun EmployeeProfilePreview() {
    PreviewSurface { EmployeeProfileScreen(previewState(), embedded = false, onBack = {}, onOpenTask = {}, onMessage = {}) }
}
