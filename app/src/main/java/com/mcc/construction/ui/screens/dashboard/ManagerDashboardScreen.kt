package com.mcc.construction.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Notification
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.MetricTile
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.format.relativeTime
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.screens.projects.ProjectCard
import com.mcc.construction.ui.screens.projects.ProjectSummary
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Where the manager dashboard can send the manager. */
sealed interface ManagerDestination {
    data object Projects : ManagerDestination
    data class Project(val projectId: Int) : ManagerDestination
    data class AssignTask(val projectId: Int) : ManagerDestination
    data object Tasks : ManagerDestination
    data object Employees : ManagerDestination
    data object Inventory : ManagerDestination
    data class Material(val materialId: Int) : ManagerDestination
    data object Reports : ManagerDestination
    data object Documents : ManagerDestination
    data object Notifications : ManagerDestination
}

@Composable
fun ManagerDashboardRoute(
    onNavigate: (ManagerDestination) -> Unit,
    viewModel: ManagerDashboardViewModel = viewModel(factory = ManagerDashboardViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ManagerDashboardScreen(state, onNavigate)
}

/**
 * The manager's overview. On a wide window it is three columns — projects,
 * where the work stands, and what needs attention — so the whole operation
 * fits on one screen. On a phone the same blocks stack, alerts first.
 */
@Composable
fun ManagerDashboardScreen(
    state: ManagerDashboardUiState,
    onNavigate: (ManagerDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }
    val wide = LocalWindowLayout.current.isExpanded
    ScreenScaffold(modifier = modifier, maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth) {
        Header(state, onNavigate, wide)
        Spacer(Modifier.height(Spacing.sectionGap))
        Metrics(state, onNavigate, wide)
        Spacer(Modifier.height(Spacing.sectionGap))
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                Column(Modifier.weight(1.15f)) { Projects(state, onNavigate) }
                Column(Modifier.weight(1f)) {
                    Updates(state, onNavigate)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    TasksByStatus(state, onNavigate)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    AttendanceCard(state, onNavigate)
                }
                Column(Modifier.weight(1f)) {
                    Alerts(state, onNavigate)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    ManagerQuickActions(state, onNavigate)
                }
            }
        } else {
            Alerts(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            Projects(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            TasksByStatus(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            AttendanceCard(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            Updates(state, onNavigate)
            Spacer(Modifier.height(Spacing.sectionGap))
            ManagerQuickActions(state, onNavigate)
        }
    }
}

@Composable
private fun Header(state: ManagerDashboardUiState, onNavigate: (ManagerDestination) -> Unit, wide: Boolean) {
    val greeting = stringResource(
        when (state.now.hour) {
            in 0..11 -> R.string.dashboard_greeting_morning
            in 12..16 -> R.string.dashboard_greeting_afternoon
            else -> R.string.dashboard_greeting_evening
        },
        state.firstName
    )
    val assign: (() -> Unit)? = state.defaultProjectId?.let { id -> { onNavigate(ManagerDestination.AssignTask(id)) } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = state.today.format(DATE_FORMAT),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (wide && assign != null) {
            PrimaryButton(
                text = stringResource(R.string.manager_assign_task),
                onClick = assign,
                icon = Icons.Filled.PersonAdd,
                modifier = Modifier.width(ASSIGN_BUTTON_WIDTH)
            )
        }
    }
    if (!wide && assign != null) {
        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(
            text = stringResource(R.string.manager_assign_task),
            onClick = assign,
            icon = Icons.Filled.PersonAdd
        )
    }
}

@Composable
private fun Metrics(state: ManagerDashboardUiState, onNavigate: (ManagerDestination) -> Unit, wide: Boolean) {
    val active = state.projects.count { it.project.status == ProjectStatus.ACTIVE }
    val tiles: List<@Composable (Modifier) -> Unit> = listOf(
        { m ->
            MetricTile(
                value = active.toString(),
                label = stringResource(R.string.manager_metric_active_projects),
                icon = Icons.Filled.Work,
                tone = Tone.Info,
                modifier = m,
                onClick = { onNavigate(ManagerDestination.Projects) }
            )
        },
        { m ->
            MetricTile(
                value = state.openTasks.toString(),
                label = stringResource(R.string.manager_metric_open_tasks),
                icon = Icons.AutoMirrored.Filled.Assignment,
                tone = if (state.overdueTasks > 0) Tone.Danger else Tone.Info,
                caption = if (state.overdueTasks > 0) {
                    pluralStringResource(R.plurals.manager_overdue, state.overdueTasks, state.overdueTasks)
                } else {
                    null
                },
                modifier = m,
                onClick = { onNavigate(ManagerDestination.Tasks) }
            )
        },
        { m ->
            MetricTile(
                value = stringResource(R.string.manager_metric_x_of_y, state.attendance.onSite, state.attendance.workforce),
                label = stringResource(R.string.manager_metric_on_site),
                icon = Icons.Filled.Groups,
                tone = Tone.Success,
                modifier = m,
                onClick = { onNavigate(ManagerDestination.Employees) }
            )
        },
        { m ->
            MetricTile(
                value = state.alerts.size.toString(),
                label = stringResource(R.string.manager_metric_alerts),
                icon = Icons.Filled.ReportProblem,
                tone = if (state.alerts.isEmpty()) Tone.Success else Tone.Warning,
                caption = if (state.reportsAwaitingApproval > 0) {
                    pluralStringResource(
                        R.plurals.manager_reports_to_approve,
                        state.reportsAwaitingApproval,
                        state.reportsAwaitingApproval
                    )
                } else {
                    null
                },
                modifier = m,
                onClick = { onNavigate(ManagerDestination.Reports) }
            )
        }
    )
    if (wide) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            tiles.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
        }
    } else {
        tiles.chunked(2).forEachIndexed { index, pair ->
            if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
            ) {
                pair.forEach { tile ->
                    tile(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun Projects(state: ManagerDashboardUiState, onNavigate: (ManagerDestination) -> Unit) {
    SectionHeader(
        title = stringResource(R.string.manager_section_projects),
        actionLabel = stringResource(R.string.dashboard_view_all),
        onAction = { onNavigate(ManagerDestination.Projects) }
    )
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.projects.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Work,
            title = stringResource(R.string.manager_no_projects_title),
            message = stringResource(R.string.manager_no_projects_message)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        state.projects.forEach { summary ->
            ProjectCard(
                summary = summary,
                today = state.today,
                onClick = { onNavigate(ManagerDestination.Project(summary.project.projectId)) }
            )
        }
    }
}

@Composable
private fun TasksByStatus(state: ManagerDashboardUiState, onNavigate: (ManagerDestination) -> Unit) {
    SectionHeader(
        title = stringResource(R.string.manager_section_tasks),
        actionLabel = stringResource(R.string.dashboard_view_all),
        onAction = { onNavigate(ManagerDestination.Tasks) }
    )
    Spacer(Modifier.height(Spacing.blockGap))
    AppCard(Modifier.fillMaxWidth(), onClick = { onNavigate(ManagerDestination.Tasks) }) {
        val total = state.totalTasks.coerceAtLeast(1)
        STATUS_ORDER.forEachIndexed { index, status ->
            if (index > 0) Spacer(Modifier.height(Spacing.md))
            val count = state.taskCounts[status] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(status.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            ProgressTrack(progress = count / total.toFloat(), tone = status.tone)
        }
    }
}

@Composable
private fun AttendanceCard(state: ManagerDashboardUiState, onNavigate: (ManagerDestination) -> Unit) {
    val a = state.attendance
    SectionHeader(
        title = stringResource(R.string.manager_section_attendance),
        actionLabel = stringResource(R.string.dashboard_view_all),
        onAction = { onNavigate(ManagerDestination.Employees) }
    )
    Spacer(Modifier.height(Spacing.blockGap))
    AppCard(Modifier.fillMaxWidth(), onClick = { onNavigate(ManagerDestination.Employees) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            AttendanceFigure(a.onSite, stringResource(R.string.presence_on_site), Tone.Success, Modifier.weight(1f))
            AttendanceFigure(a.finished, stringResource(R.string.presence_finished), Tone.Info, Modifier.weight(1f))
            AttendanceFigure(a.notIn, stringResource(R.string.presence_not_in), Tone.Warning, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.md))
        AttendanceBar(a)
        if (a.notInNames.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.manager_not_in_names, a.notInNames.joinToString(", ")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttendanceFigure(value: Int, label: String, tone: Tone, modifier: Modifier) {
    Column(modifier) {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = tone.colors().ink)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One bar split into on site, finished and not in. */
@Composable
private fun AttendanceBar(a: AttendanceSummary) {
    val total = a.workforce.coerceAtLeast(1).toFloat()
    Row(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        listOf(a.onSite to Tone.Success, a.finished to Tone.Info, a.notIn to Tone.Warning).forEach { (count, tone) ->
            if (count > 0) {
                Box(
                    Modifier
                        .weight(count / total)
                        .height(10.dp)
                        .background(tone.colors().figure)
                )
            }
        }
    }
}

@Composable
private fun Alerts(state: ManagerDashboardUiState, onNavigate: (ManagerDestination) -> Unit) {
    SectionHeader(title = stringResource(R.string.manager_section_alerts))
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.alerts.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.VerifiedUser,
            title = stringResource(R.string.manager_no_alerts_title),
            message = stringResource(R.string.manager_no_alerts_message),
            tone = Tone.Success
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.alerts.take(ALERTS_SHOWN).forEach { alert -> AlertRow(alert, onNavigate) }
        if (state.alerts.size > ALERTS_SHOWN) {
            val more = state.alerts.size - ALERTS_SHOWN
            Text(
                text = pluralStringResource(R.plurals.manager_more_alerts, more, more),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlertRow(alert: DashboardAlert, onNavigate: (ManagerDestination) -> Unit) {
    when (alert) {
        is DashboardAlert.Incident -> ListRow(
            title = stringResource(R.string.manager_alert_incident, stringResource(alert.incident.severity.labelRes)),
            subtitle = alert.incident.description,
            meta = alert.projectName,
            onClick = { onNavigate(ManagerDestination.Reports) },
            leading = { LeadingIcon(Icons.Filled.ReportProblem, alert.incident.severity.tone) }
        )

        is DashboardAlert.FailedInspection -> ListRow(
            title = stringResource(R.string.manager_alert_failed_inspection),
            subtitle = stringResource(R.string.manager_alert_by, alert.inspectorName),
            meta = alert.projectName,
            onClick = { onNavigate(ManagerDestination.Reports) },
            leading = { LeadingIcon(Icons.Filled.VerifiedUser, Tone.Danger) }
        )

        is DashboardAlert.LowStock -> ListRow(
            title = alert.item.materialName,
            subtitle = stringResource(
                R.string.manager_alert_stock,
                stringResource(alert.item.stockLevel.labelRes),
                alert.item.quantityAvailable,
                alert.item.unitType,
                alert.item.minimumQuantity
            ),
            meta = alert.projectName,
            onClick = { onNavigate(ManagerDestination.Material(alert.item.materialId)) },
            leading = { LeadingIcon(Icons.Filled.Inventory2, alert.item.stockLevel.tone) }
        )
    }
}

@Composable
private fun Updates(state: ManagerDashboardUiState, onNavigate: (ManagerDestination) -> Unit) {
    SectionHeader(
        title = stringResource(R.string.manager_section_updates),
        actionLabel = stringResource(R.string.dashboard_view_all),
        onAction = { onNavigate(ManagerDestination.Notifications) }
    )
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.updates.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.NotificationsNone,
            title = stringResource(R.string.manager_no_updates_title),
            message = stringResource(R.string.manager_no_updates_message)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.updates.forEach { update -> UpdateRow(update, state.now) { onNavigate(update.destination()) } }
    }
}

/** An update in full: these are the workers' own progress reports, so none of it is cut off. */
@Composable
private fun UpdateRow(update: Notification, now: LocalDateTime, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            LeadingIcon(update.type.icon, update.type.tone)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = update.messageContent,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = relativeTime(update.sentDate, now),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!update.isRead) {
                Spacer(Modifier.width(Spacing.sm))
                Box(Modifier.padding(top = Spacing.sm)) { UnreadDot() }
            }
        }
    }
}

@Composable
private fun UnreadDot() {
    Box(
        Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
    )
}

private fun Notification.destination(): ManagerDestination = when (type) {
    NotificationType.TASK -> ManagerDestination.Tasks
    NotificationType.INVENTORY -> ManagerDestination.Inventory
    NotificationType.REPORT, NotificationType.SAFETY -> ManagerDestination.Reports
    else -> ManagerDestination.Notifications
}

@Composable
private fun ManagerQuickActions(state: ManagerDashboardUiState, onNavigate: (ManagerDestination) -> Unit) {
    SectionHeader(title = stringResource(R.string.dashboard_quick_actions))
    Spacer(Modifier.height(Spacing.blockGap))
    val actions = buildList {
        state.defaultProjectId?.let { add(ManagerQuickAction(Icons.Filled.PersonAdd, R.string.manager_assign_task, ManagerDestination.AssignTask(it))) }
        add(ManagerQuickAction(Icons.Filled.Assessment, R.string.manager_action_review_reports, ManagerDestination.Reports))
        add(ManagerQuickAction(Icons.Filled.Inventory2, R.string.manager_action_check_stock, ManagerDestination.Inventory))
        add(ManagerQuickAction(Icons.Filled.UploadFile, R.string.manager_action_upload, ManagerDestination.Documents))
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        actions.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
                pair.forEach { action ->
                    ManagerQuickActionTile(action, Modifier.weight(1f)) { onNavigate(action.destination) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class ManagerQuickAction(val icon: ImageVector, val labelRes: Int, val destination: ManagerDestination)

@Composable
private fun ManagerQuickActionTile(action: ManagerQuickAction, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier = modifier.heightIn(min = Spacing.rowHeight), onClick = onClick) {
        Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(action.labelRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val STATUS_ORDER = listOf(TaskStatus.IN_PROGRESS, TaskStatus.PENDING, TaskStatus.BLOCKED, TaskStatus.COMPLETED)
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val WIDE_CONTENT: Dp = 1320.dp
private val ASSIGN_BUTTON_WIDTH: Dp = 240.dp
private const val ALERTS_SHOWN = 5

internal fun previewManagerDashboard(): ManagerDashboardUiState {
    val today = LocalDate.now()
    val now = LocalDateTime.now().withHour(9)
    fun project(id: Int, name: String, place: String, status: ProjectStatus, progress: Double, endIn: Long) =
        Project(id, 2, name, place, BigDecimal("48200000"), today.minusDays(200), today.plusDays(endIn), status, progress)
    return ManagerDashboardUiState(
        isLoading = false,
        firstName = "Naledi",
        today = today,
        now = now,
        projects = listOf(
            ProjectSummary(project(1, "Riverside Estate Phase 2", "Centurion, Gauteng", ProjectStatus.ACTIVE, 68.0, 68), 7, 1, 11),
            ProjectSummary(project(2, "Harbour View Offices", "Foreshore, Cape Town", ProjectStatus.ACTIVE, 41.5, 512), 3, 0, 4),
            ProjectSummary(project(3, "N3 Bridge Rehabilitation", "Pietermaritzburg", ProjectStatus.ON_HOLD, 22.0, 202), 1, 0, 1)
        ),
        taskCounts = mapOf(TaskStatus.PENDING to 5, TaskStatus.IN_PROGRESS to 4, TaskStatus.BLOCKED to 1, TaskStatus.COMPLETED to 6),
        overdueTasks = 2,
        attendance = AttendanceSummary(onSite = 5, finished = 1, notIn = 2, notInNames = listOf("Anele", "Kagiso")),
        alerts = listOf(
            DashboardAlert.Incident(
                IncidentReport(1, 1, 3, "Loose scaffold board on level 2, east side.", now.minusHours(3)),
                "Riverside Estate Phase 2"
            ),
            DashboardAlert.FailedInspection(SafetyInspection(4, 1, 6, today, "Failed: harness", false), "Thabo Mokoena", "Riverside Estate Phase 2"),
            DashboardAlert.LowStock(Inventory(4, 2, 4, "Torch-on waterproofing membrane", 0, 15, "rolls", BigDecimal.TEN), "Harbour View Offices")
        ),
        reportsAwaitingApproval = 2,
        updates = listOf(
            Notification(1, 2, NotificationType.TASK, "Sipho Dlamini started “Erect scaffolding — east elevation”.", now.minusMinutes(8), false),
            Notification(2, 2, NotificationType.REPORT, "Thabo Mokoena submitted a daily report for Riverside Estate Phase 2.", now.minusHours(2), true)
        ),
        defaultProjectId = 1
    )
}

@DevicePreviews
@Composable
private fun ManagerDashboardPreview() {
    PreviewSurface { ManagerDashboardScreen(previewManagerDashboard(), onNavigate = {}) }
}
