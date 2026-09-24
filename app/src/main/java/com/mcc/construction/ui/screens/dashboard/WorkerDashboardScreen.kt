package com.mcc.construction.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AccentButton
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.format.elapsedHoursMinutes
import com.mcc.construction.ui.format.formatTime
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.screens.tasks.TaskCard
import com.mcc.construction.ui.screens.tasks.TaskListItem
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** The places the dashboard's shortcuts lead. */
enum class WorkerDestination { TASKS, ATTENDANCE, DAILY_REPORT, SAFETY_CHECKLIST, INCIDENT_REPORT, NOTIFICATIONS }

/** Worker dashboard wired to its ViewModel. */
@Composable
fun WorkerDashboardRoute(
    onNavigate: (WorkerDestination) -> Unit,
    onOpenTask: (Int) -> Unit,
    viewModel: WorkerDashboardViewModel = viewModel(factory = WorkerDashboardViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkerDashboardScreen(
        state = state,
        onClockInOut = viewModel::onClockInOut,
        onNavigate = onNavigate,
        onOpenTask = onOpenTask,
        onMessageShown = viewModel::onMessageShown
    )
}

/**
 * Everything a worker needs at the start of a shift, one tap away: clock in,
 * today's tasks, the daily report, the safety checklist and incident
 * reporting. On a wide window the actions sit on the left and today's work on
 * the right.
 */
@Composable
fun WorkerDashboardScreen(
    state: WorkerDashboardUiState,
    onClockInOut: () -> Unit,
    onNavigate: (WorkerDestination) -> Unit,
    onOpenTask: (Int) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }
    val wide = LocalWindowLayout.current.isExpanded
    ActionFeedbackHost(state.message, onMessageShown, modifier) {
        ScreenScaffold(maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth) {
            Greeting(state)
            Spacer(Modifier.height(Spacing.xl))
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                    Column(Modifier.weight(1f)) {
                        ClockCard(state, onClockInOut)
                        Spacer(Modifier.height(Spacing.sectionGap))
                        QuickActions(state, onNavigate, columns = 2)
                    }
                    Column(Modifier.weight(1f)) {
                        TodaySummary(state, onNavigate)
                        Spacer(Modifier.height(Spacing.sectionGap))
                        NotificationsRow(state, onNavigate)
                        NextTasks(state, onNavigate, onOpenTask)
                    }
                }
            } else {
                ClockCard(state, onClockInOut)
                Spacer(Modifier.height(Spacing.lg))
                TodaySummary(state, onNavigate)
                Spacer(Modifier.height(Spacing.sectionGap))
                QuickActions(state, onNavigate, columns = 2)
                Spacer(Modifier.height(Spacing.sectionGap))
                NotificationsRow(state, onNavigate)
                NextTasks(state, onNavigate, onOpenTask)
            }
        }
    }
}

@Composable
private fun Greeting(state: WorkerDashboardUiState) {
    Text(
        text = stringResource(
            when (state.dayPart) {
                DayPart.MORNING -> R.string.dashboard_greeting_morning
                DayPart.AFTERNOON -> R.string.dashboard_greeting_afternoon
                DayPart.EVENING -> R.string.dashboard_greeting_evening
            },
            state.firstName
        ),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(Spacing.xxs))
    val date = state.today.format(DATE_FORMAT)
    Text(
        text = state.siteName?.let { stringResource(R.string.dashboard_date_and_site, date, it) } ?: date,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Clock-in status and the one big button that changes it. */
@Composable
private fun ClockCard(state: WorkerDashboardUiState, onClockInOut: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        val shift = state.openShift
        if (shift != null) {
            StatusChip(
                text = stringResource(R.string.attendance_on_shift_since, shift.clockIn.formatTime()),
                tone = Tone.Success,
                icon = Icons.Filled.CheckCircle
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = shiftElapsedText(shift),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(
                text = stringResource(R.string.attendance_clock_out),
                icon = Icons.AutoMirrored.Filled.Logout,
                loading = state.isClockUpdating,
                onClick = onClockInOut
            )
        } else {
            StatusChip(
                text = stringResource(R.string.attendance_not_clocked_in),
                tone = Tone.Neutral,
                icon = Icons.Filled.Schedule
            )
            Spacer(Modifier.height(Spacing.lg))
            // Clocking in is the first thing a worker does on site, so it gets
            // the one gold button on the screen.
            AccentButton(
                text = stringResource(
                    if (state.isClockUpdating) R.string.attendance_working else R.string.attendance_clock_in
                ),
                icon = Icons.AutoMirrored.Filled.Login,
                onClick = { if (!state.isClockUpdating) onClockInOut() }
            )
        }
    }
}

/** "On shift for 2 h 15 min", refreshed every half minute. */
@Composable
fun shiftElapsedText(shift: Attendance): String {
    val now by produceState(LocalDateTime.now(), shift.clockIn) {
        while (true) {
            value = LocalDateTime.now()
            delay(TICK_MILLIS)
        }
    }
    val (hours, minutes) = elapsedHoursMinutes(shift.clockIn, now)
    return stringResource(R.string.attendance_on_shift_for, hours, minutes)
}

@Composable
private fun TodaySummary(state: WorkerDashboardUiState, onNavigate: (WorkerDestination) -> Unit) {
    SectionHeader(title = stringResource(R.string.dashboard_today))
    Spacer(Modifier.height(Spacing.blockGap))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        SummaryTile(state.inProgressCount, R.string.dashboard_in_progress, Icons.Filled.PlayCircle, Tone.Info,
            { onNavigate(WorkerDestination.TASKS) }, Modifier.weight(1f))
        SummaryTile(state.pendingCount, R.string.dashboard_to_do, Icons.Filled.Schedule, Tone.Warning,
            { onNavigate(WorkerDestination.TASKS) }, Modifier.weight(1f))
        SummaryTile(state.completedCount, R.string.dashboard_done, Icons.Filled.TaskAlt, Tone.Success,
            { onNavigate(WorkerDestination.TASKS) }, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryTile(
    count: Int,
    labelRes: Int,
    icon: ImageVector,
    tone: Tone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = tone.colors()
    AppCard(modifier = modifier, onClick = onClick) {
        Icon(icon, contentDescription = null, tint = c.figure, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActions(
    state: WorkerDashboardUiState,
    onNavigate: (WorkerDestination) -> Unit,
    columns: Int
) {
    SectionHeader(title = stringResource(R.string.dashboard_quick_actions))
    Spacer(Modifier.height(Spacing.blockGap))
    val actions = listOf(
        QuickAction(R.string.nav_tasks, Icons.AutoMirrored.Filled.Assignment, Tone.Info, WorkerDestination.TASKS,
            badge = if (state.openCount > 0) pluralStringResource(R.plurals.dashboard_open_tasks, state.openCount, state.openCount) else null),
        QuickAction(R.string.nav_daily_report, Icons.Filled.Description, Tone.Info, WorkerDestination.DAILY_REPORT),
        QuickAction(R.string.title_safety_checklist, Icons.Filled.HealthAndSafety,
            if (state.checklistDoneToday) Tone.Success else Tone.Warning, WorkerDestination.SAFETY_CHECKLIST,
            badge = stringResource(if (state.checklistDoneToday) R.string.dashboard_checklist_done else R.string.dashboard_checklist_due)),
        QuickAction(R.string.dashboard_report_incident, Icons.Filled.ReportProblem, Tone.Danger, WorkerDestination.INCIDENT_REPORT)
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        actions.chunked(columns).forEach { row ->
            // Intrinsic height so both tiles in a row match the taller one.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
            ) {
                row.forEach { action ->
                    QuickActionTile(
                        action,
                        onClick = { onNavigate(action.destination) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private data class QuickAction(
    val labelRes: Int,
    val icon: ImageVector,
    val tone: Tone,
    val destination: WorkerDestination,
    val badge: String? = null
)

/** A big square shortcut — well over the 56dp primary-action size, for gloved hands. */
@Composable
private fun QuickActionTile(action: QuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = action.tone.colors()
    Column(
        modifier = modifier
            .heightIn(min = TILE_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.container),
            contentAlignment = Alignment.Center
        ) {
            Icon(action.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(action.labelRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (action.badge != null) {
            Text(
                text = action.badge,
                style = MaterialTheme.typography.bodySmall,
                color = c.ink,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun NotificationsRow(state: WorkerDashboardUiState, onNavigate: (WorkerDestination) -> Unit) {
    if (state.unreadNotifications == 0) return
    ListRow(
        title = pluralStringResource(
            R.plurals.dashboard_unread_notifications,
            state.unreadNotifications,
            state.unreadNotifications
        ),
        subtitle = stringResource(R.string.dashboard_unread_notifications_hint),
        onClick = { onNavigate(WorkerDestination.NOTIFICATIONS) },
        leading = { LeadingIcon(Icons.Filled.Notifications, Tone.Warning) },
        trailing = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
    Spacer(Modifier.height(Spacing.sectionGap))
}

@Composable
private fun NextTasks(
    state: WorkerDashboardUiState,
    onNavigate: (WorkerDestination) -> Unit,
    onOpenTask: (Int) -> Unit
) {
    SectionHeader(
        title = stringResource(R.string.dashboard_next_up),
        actionLabel = stringResource(R.string.dashboard_view_all),
        onAction = { onNavigate(WorkerDestination.TASKS) }
    )
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.nextTasks.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.TaskAlt,
            title = stringResource(R.string.dashboard_all_done_title),
            message = stringResource(R.string.dashboard_all_done_message),
            tone = Tone.Success
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        state.nextTasks.forEach { item ->
            TaskCard(item = item, today = state.today, onClick = { onOpenTask(item.task.taskId) })
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val TILE_HEIGHT: Dp = 112.dp
private val WIDE_CONTENT: Dp = 1120.dp
private const val TICK_MILLIS = 30_000L

private fun previewState(clockedIn: Boolean): WorkerDashboardUiState {
    val today = LocalDate.now()
    return WorkerDashboardUiState(
        isLoading = false,
        firstName = "Sipho",
        dayPart = DayPart.MORNING,
        today = today,
        siteName = "Riverside Estate Phase 2",
        pendingCount = 3,
        inProgressCount = 1,
        completedCount = 1,
        nextTasks = listOf(
            TaskListItem(Task(101, 1, "Pour concrete foundation — Unit 4", "", today, TaskPriority.HIGH, TaskStatus.IN_PROGRESS), "Riverside Estate Phase 2"),
            TaskListItem(Task(102, 1, "Erect scaffolding — east elevation", "", today, TaskPriority.MEDIUM, TaskStatus.PENDING), "Riverside Estate Phase 2")
        ),
        openShift = if (clockedIn) Attendance(1, 5, today, LocalDateTime.now().minusMinutes(135), null, null) else null,
        unreadNotifications = 2
    )
}

@DevicePreviews
@Composable
private fun WorkerDashboardPreview() {
    PreviewSurface {
        WorkerDashboardScreen(previewState(clockedIn = false), {}, {}, {}, {})
    }
}

@DevicePreviews
@Composable
private fun WorkerDashboardClockedInPreview() {
    PreviewSurface {
        WorkerDashboardScreen(previewState(clockedIn = true), {}, {}, {}, {})
    }
}
