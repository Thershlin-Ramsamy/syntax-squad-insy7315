package com.mcc.construction.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.DocumentCategory
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.format.formatRand
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.screens.documents.icon
import com.mcc.construction.ui.screens.documents.labelRes
import com.mcc.construction.ui.screens.employees.AttendanceToday
import com.mcc.construction.ui.screens.employees.PresenceStatus
import com.mcc.construction.ui.screens.employees.labelRes
import com.mcc.construction.ui.screens.employees.tone
import com.mcc.construction.ui.screens.tasks.deadlineText
import com.mcc.construction.ui.screens.tasks.isOverdue
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** What Project Details can open. */
data class ProjectDetailActions(
    val onBack: () -> Unit = {},
    val onAssignTask: (projectId: Int) -> Unit = {},
    val onOpenTask: (taskId: Int) -> Unit = {},
    val onOpenEmployee: (userId: Int) -> Unit = {},
    val onOpenMaterial: (materialId: Int) -> Unit = {},
    val onOpenDocuments: () -> Unit = {},
    val onOpenReports: () -> Unit = {}
)

/**
 * Project Details wired to its ViewModel.
 *
 * @param embedded true when shown in the projects list's side pane: no app bar
 *   of its own, and one column because the pane is narrower.
 */
@Composable
fun ProjectDetailRoute(
    projectId: Int,
    actions: ProjectDetailActions,
    embedded: Boolean = false,
    viewModel: ProjectDetailViewModel = viewModel(
        key = "project-detail-$projectId",
        factory = ProjectDetailViewModel.factory(projectId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProjectDetailScreen(state, actions, embedded)
}

/**
 * One project: overview with budget, dates and progress, then its tasks, team,
 * materials, documents and latest daily reports. "Assign a task" is the one
 * primary action.
 */
@Composable
fun ProjectDetailScreen(
    state: ProjectDetailUiState,
    actions: ProjectDetailActions,
    embedded: Boolean,
    modifier: Modifier = Modifier
) {
    val project = state.project
    val twoColumns = LocalWindowLayout.current.isExpanded && !embedded
    ScreenScaffold(
        modifier = modifier,
        title = if (embedded) null else project?.projectName ?: stringResource(R.string.project_detail_title),
        onBack = if (embedded) null else actions.onBack,
        maxContentWidth = if (twoColumns) WIDE_CONTENT else Spacing.contentMaxWidth
    ) {
        when {
            state.isLoading -> LoadingState()
            project == null -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(R.string.project_not_found_title),
                message = stringResource(R.string.project_not_found_message)
            )
            twoColumns -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                Column(Modifier.weight(1f)) {
                    Overview(project, state, actions)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Tasks(state, actions)
                }
                Column(Modifier.weight(1f)) {
                    Team(state, actions)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Materials(state, actions)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Documents(state, actions)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Reports(state, actions)
                }
            }
            else -> {
                if (embedded) {
                    Text(project.projectName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(Spacing.md))
                }
                Overview(project, state, actions)
                Spacer(Modifier.height(Spacing.sectionGap))
                Tasks(state, actions)
                Spacer(Modifier.height(Spacing.sectionGap))
                Team(state, actions)
                Spacer(Modifier.height(Spacing.sectionGap))
                Materials(state, actions)
                Spacer(Modifier.height(Spacing.sectionGap))
                Documents(state, actions)
                Spacer(Modifier.height(Spacing.sectionGap))
                Reports(state, actions)
            }
        }
    }
}

@Composable
private fun Overview(project: Project, state: ProjectDetailUiState, actions: ProjectDetailActions) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(stringResource(project.status.labelRes), project.status.tone)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = project.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.project_progress_percent, project.progressLabel),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.project_complete),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        ProgressTrack(progress = project.progressFraction, tone = project.status.tone)
        Spacer(Modifier.height(Spacing.lg))
        KeyValueRow(stringResource(R.string.project_budget), project.budget.formatRand())
        Spacer(Modifier.height(Spacing.sm))
        KeyValueRow(stringResource(R.string.project_start), project.startDate.formatDate())
        Spacer(Modifier.height(Spacing.sm))
        KeyValueRow(
            label = stringResource(R.string.project_end),
            value = project.endDate.formatDate(),
            valueColor = deadlineInk(project, state.today)
        )
        Spacer(Modifier.height(Spacing.sm))
        KeyValueRow(stringResource(R.string.project_time_left), timeLeft(project, state.today))
        if (state.managerName.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            KeyValueRow(stringResource(R.string.project_manager), state.managerName)
        }
    }
    if (project.status != ProjectStatus.COMPLETED) {
        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(
            text = stringResource(R.string.manager_assign_task),
            onClick = { actions.onAssignTask(project.projectId) },
            icon = Icons.Filled.PersonAdd
        )
    }
}

@Composable
private fun deadlineInk(project: Project, today: LocalDate): Color {
    val tone = project.deadlineTone(today)
    return if (tone == Tone.Neutral) MaterialTheme.colorScheme.onSurface else tone.colors().ink
}

@Composable
private fun timeLeft(project: Project, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, project.endDate).toInt()
    return when {
        project.status == ProjectStatus.COMPLETED -> stringResource(R.string.project_finished)
        project.startDate.isAfter(today) -> stringResource(R.string.project_starts_on, project.startDate.formatDate())
        days < 0 -> pluralStringResource(R.plurals.project_days_over, -days, -days)
        else -> pluralStringResource(R.plurals.project_days_left, days, days)
    }
}

@Composable
private fun Tasks(state: ProjectDetailUiState, actions: ProjectDetailActions) {
    val open = state.tasks.count { it.task.status != TaskStatus.COMPLETED }
    SectionHeader(title = pluralStringResource(R.plurals.project_section_tasks, open, open, state.tasks.size))
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.tasks.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.Assignment,
            title = stringResource(R.string.project_no_tasks_title),
            message = stringResource(R.string.project_no_tasks_message)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.tasks.forEach { item ->
            val task = item.task
            val assignees = item.assignees.joinToString(", ") { it.firstName }
            ListRow(
                title = task.title,
                subtitle = assignees.ifEmpty { stringResource(R.string.project_task_unassigned) },
                meta = deadlineText(task.deadline, state.today, task.status),
                onClick = { actions.onOpenTask(task.taskId) },
                trailing = {
                    StatusChip(
                        text = stringResource(task.status.labelRes),
                        tone = if (isOverdue(task, state.today)) Tone.Danger else task.status.tone
                    )
                }
            )
        }
    }
}

@Composable
private fun Team(state: ProjectDetailUiState, actions: ProjectDetailActions) {
    val onSite = state.team.count { it.today.status == PresenceStatus.ON_SITE }
    SectionHeader(title = stringResource(R.string.project_section_team, onSite, state.team.size))
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.team.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Groups,
            title = stringResource(R.string.project_no_team_title),
            message = stringResource(R.string.project_no_team_message)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.team.forEach { member ->
            ListRow(
                title = member.user.fullName,
                subtitle = member.user.jobTitle,
                meta = pluralStringResource(R.plurals.project_member_tasks, member.openTasks, member.openTasks),
                onClick = { actions.onOpenEmployee(member.user.userId) },
                leading = { Avatar(member.user.fullName) },
                trailing = { StatusPill(stringResource(member.today.status.labelRes), member.today.status.tone, showDot = true) }
            )
        }
    }
}

@Composable
private fun Materials(state: ProjectDetailUiState, actions: ProjectDetailActions) {
    SectionHeader(title = stringResource(R.string.project_section_materials))
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.materials.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Inventory2,
            title = stringResource(R.string.project_no_materials_title),
            message = stringResource(R.string.project_no_materials_message)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.materials.forEach { item ->
            ListRow(
                title = item.materialName,
                subtitle = stringResource(R.string.inventory_quantity, item.quantityAvailable, item.unitType),
                onClick = { actions.onOpenMaterial(item.materialId) },
                leading = { LeadingIcon(Icons.Filled.Inventory2, item.stockLevel.tone) },
                trailing = { StatusChip(stringResource(item.stockLevel.labelRes), item.stockLevel.tone) }
            )
        }
    }
}

@Composable
private fun Documents(state: ProjectDetailUiState, actions: ProjectDetailActions) {
    SectionHeader(
        title = stringResource(R.string.project_section_documents),
        actionLabel = stringResource(R.string.dashboard_view_all),
        onAction = actions.onOpenDocuments
    )
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.documents.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.FolderOpen,
            title = stringResource(R.string.project_no_documents_title),
            message = stringResource(R.string.project_no_documents_message)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.documents.take(DOCUMENTS_SHOWN).forEach { doc ->
            ListRow(
                title = doc.fileName,
                subtitle = stringResource(
                    R.string.document_version_date,
                    stringResource(doc.category.labelRes),
                    doc.version,
                    doc.uploadDate.formatDate()
                ),
                onClick = actions.onOpenDocuments,
                leading = { LeadingIcon(doc.category.icon, Tone.Info) }
            )
        }
    }
}

@Composable
private fun Reports(state: ProjectDetailUiState, actions: ProjectDetailActions) {
    SectionHeader(
        title = stringResource(R.string.project_section_reports),
        actionLabel = stringResource(R.string.dashboard_view_all),
        onAction = actions.onOpenReports
    )
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.reports.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Description,
            title = stringResource(R.string.project_no_reports_title),
            message = stringResource(R.string.project_no_reports_message)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.reports.forEach { item ->
            ListRow(
                title = item.report.workCompleted,
                subtitle = item.authorName,
                meta = item.report.date.formatDate(),
                onClick = actions.onOpenReports,
                leading = { LeadingIcon(Icons.Filled.Description, item.report.status.tone) },
                trailing = { StatusChip(stringResource(item.report.status.labelRes), item.report.status.tone) }
            )
        }
    }
}

private val WIDE_CONTENT: Dp = 1200.dp
private const val DOCUMENTS_SHOWN = 4

internal fun previewProjectDetail(): ProjectDetailUiState {
    val today = LocalDate.now()
    val now = LocalDateTime.now()
    val project = Project(
        1, 2, "Riverside Estate Phase 2", "Centurion, Gauteng", BigDecimal("48200000"),
        today.minusDays(255), today.plusDays(68), ProjectStatus.ACTIVE, 68.0
    )
    val sipho = User(5, UserRole.WORKER, "Sipho", "Dlamini", "worker@mcc.co.za", "+27 82 445 1120", "Site Operative", "MCC-2041")
    val ayanda = User(10, UserRole.WORKER, "Ayanda", "Nkosi", "ayanda@mcc.co.za", "+27 72 118 2044", "Steel Fixer", "MCC-2077")
    return ProjectDetailUiState(
        isLoading = false,
        project = project,
        managerName = "Naledi Khumalo",
        tasks = listOf(
            ProjectTask(Task(101, 1, "Pour concrete foundation — Unit 4", "", today, TaskPriority.HIGH, TaskStatus.IN_PROGRESS), listOf(sipho, ayanda)),
            ProjectTask(Task(102, 1, "Erect scaffolding — east elevation", "", today.plusDays(1), TaskPriority.MEDIUM, TaskStatus.PENDING), listOf(sipho))
        ),
        team = listOf(
            TeamMember(sipho, AttendanceToday(PresenceStatus.ON_SITE), 2),
            TeamMember(ayanda, AttendanceToday.NotIn, 1)
        ),
        materials = listOf(
            Inventory(2, 1, 2, "Rebar Y12 (6 m lengths)", 84, 90, "bundles", BigDecimal("742")),
            Inventory(1, 1, 1, "Portland cement CEM II 42.5N (50 kg)", 240, 100, "bags", BigDecimal("118.50"))
        ),
        documents = listOf(Document(1, 1, "Riverside-Structural-Drawings-RevC.pdf", 3, now.minusDays(13), DocumentCategory.DRAWING)),
        reports = listOf(
            ProjectReport(DailyReport(1, 1, 5, today.minusDays(1), "Sunny", "Blockwork to ground floor, units 3 and 4.", "", ReportStatus.SUBMITTED), "Sipho Dlamini")
        ),
        today = today
    )
}

@DevicePreviews
@Composable
private fun ProjectDetailPreview() {
    PreviewSurface { ProjectDetailScreen(previewProjectDetail(), ProjectDetailActions(), embedded = false) }
}

@DevicePreviews
@Composable
private fun ProjectDetailEmbeddedPreview() {
    PreviewSurface { ProjectDetailScreen(previewProjectDetail(), ProjectDetailActions(), embedded = true) }
}
