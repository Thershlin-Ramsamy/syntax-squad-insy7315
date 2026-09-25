package com.mcc.construction.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListDetailLayout
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PhotoAttachment
import com.mcc.construction.ui.components.PhotoAttachmentRow
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.components.SwitchRow
import com.mcc.construction.ui.components.selectedOutline
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.format.relativeTime
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/** Callbacks for the reports history. */
data class ReportsActions(
    val onProjectFilterChange: (Int?) -> Unit = {},
    val onTypeFilterChange: (ReportType?) -> Unit = {},
    val onAwaitingOnlyChange: (Boolean) -> Unit = {},
    val onEntryClick: (String) -> Unit = {},
    val onApprove: (Int) -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

@Composable
fun ReportsRoute(viewModel: ReportsViewModel = viewModel(factory = ReportsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val twoPane = LocalWindowLayout.current.isExpanded
    ReportsScreen(
        state = state,
        twoPane = twoPane,
        actions = ReportsActions(
            onProjectFilterChange = viewModel::onProjectFilterChange,
            onTypeFilterChange = viewModel::onTypeFilterChange,
            onAwaitingOnlyChange = viewModel::onAwaitingOnlyChange,
            onEntryClick = { key -> if (twoPane) viewModel.onSelect(key) else viewModel.onToggleExpanded(key) },
            onApprove = viewModel::onApprove,
            onMessageShown = viewModel::onMessageShown
        )
    )
}

/**
 * Daily reports, safety inspections and incidents, filtered by project and
 * type. On a wide window the chosen record opens beside the list; on a phone
 * it expands in place. Submitted daily reports can be approved from either.
 */
@Composable
fun ReportsScreen(
    state: ReportsUiState,
    twoPane: Boolean,
    actions: ReportsActions,
    modifier: Modifier = Modifier
) {
    if (state.entries == null) {
        LoadingState(modifier)
        return
    }
    ActionFeedbackHost(state.message, actions.onMessageShown, modifier) {
        if (!twoPane) {
            ScreenScaffold(scrollable = false) { ReportColumn(state, actions, twoPane = false) }
        } else {
            ListDetailLayout(
                selected = state.paneEntry,
                emptyIcon = Icons.Filled.Description,
                emptyTitle = stringResource(R.string.reports_pick_title),
                emptyMessage = stringResource(R.string.reports_pick_message),
                list = { ReportColumn(state, actions, twoPane = true) },
                detail = { entry ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = Spacing.xxl, bottom = Spacing.xxxl)
                    ) {
                        EntryHeader(entry, state)
                        Spacer(Modifier.height(Spacing.lg))
                        EntryDetail(entry, state, actions)
                    }
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.ReportColumn(state: ReportsUiState, actions: ReportsActions, twoPane: Boolean) {
    val selected = if (twoPane) state.paneKey else null
    // Filters scroll with the list, so a short window still shows reports.
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item(key = "filters") { ReportFilters(state, actions) }
        if (state.visible.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.Description,
                    title = stringResource(if (state.awaitingOnly) R.string.reports_none_awaiting_title else R.string.reports_empty_title),
                    message = stringResource(if (state.awaitingOnly) R.string.reports_none_awaiting_message else R.string.reports_empty_message),
                    tone = if (state.awaitingOnly) Tone.Success else Tone.Neutral
                )
            }
        }
        items(state.visible, key = { it.key }) { entry ->
            val expanded = !twoPane && entry.key in state.expanded
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectedOutline(entry.key == selected),
                onClick = { actions.onEntryClick(entry.key) }
            ) {
                EntryHeader(entry, state, showChevron = !twoPane, expanded = expanded)
                if (expanded) {
                    Spacer(Modifier.height(Spacing.md))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(Spacing.md))
                    EntryDetail(entry, state, actions)
                }
            }
        }
    }
}

@Composable
private fun ReportFilters(state: ReportsUiState, actions: ReportsActions) {
    Column {
        ChoiceChipGroup(
            options = listOf<Int?>(null) + state.projects.map { it.projectId },
            selected = state.projectFilter,
            onSelect = actions.onProjectFilterChange,
            label = { id ->
                if (id == null) stringResource(R.string.filter_all_projects) else state.projects.first { it.projectId == id }.projectName
            }
        )
        Spacer(Modifier.height(Spacing.sm))
        ChoiceChipGroup(
            options = listOf(null) + ReportType.entries,
            selected = state.typeFilter,
            onSelect = actions.onTypeFilterChange,
            label = { type ->
                val name = if (type == null) stringResource(R.string.filter_all) else stringResource(type.labelRes)
                stringResource(R.string.filter_with_count, name, state.countFor(type))
            },
            icon = { type -> type?.icon ?: Icons.Filled.Description }
        )
        if (state.canApprove) {
            Spacer(Modifier.height(Spacing.sm))
            SwitchRow(
                title = stringResource(R.string.reports_awaiting_only),
                subtitle = pluralStringResource(R.plurals.reports_awaiting_count, state.awaitingCount, state.awaitingCount),
                checked = state.awaitingOnly,
                onCheckedChange = actions.onAwaitingOnlyChange,
                icon = Icons.Filled.HourglassTop,
                tone = Tone.Warning
            )
        }
    }
}

@Composable
private fun EntryHeader(entry: ReportEntry, state: ReportsUiState, showChevron: Boolean = false, expanded: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LeadingIcon(entry.type.icon, entry.tone)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.reports_row_subtitle, entry.authorName, entry.projectName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(entry.statusText(), entry.tone)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = when (entry) {
                        is ReportEntry.Incident -> relativeTime(entry.time, state.now)
                        else -> entry.time.toLocalDate().formatDate()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showChevron) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.reports_collapse else R.string.reports_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntryDetail(entry: ReportEntry, state: ReportsUiState, actions: ReportsActions) {
    when (entry) {
        is ReportEntry.Daily -> {
            val report = entry.report
            KeyValueRow(stringResource(R.string.reports_weather), report.weather)
            DetailBlock(stringResource(R.string.reports_work_completed), report.workCompleted)
            DetailBlock(
                stringResource(R.string.reports_problems),
                report.problemsEncountered.ifBlank { stringResource(R.string.reports_no_problems) }
            )
            Photos(report.photoUris)
            if (report.status == ReportStatus.SUBMITTED && state.canApprove) {
                Spacer(Modifier.height(Spacing.lg))
                PrimaryButton(
                    text = stringResource(R.string.reports_approve),
                    onClick = { actions.onApprove(report.reportId) },
                    icon = Icons.Filled.CheckCircle,
                    loading = report.reportId in state.approving
                )
            }
        }

        is ReportEntry.Inspection -> {
            DetailBlock(
                stringResource(R.string.reports_comments),
                entry.inspection.comments.ifBlank { stringResource(R.string.reports_no_comments) }
            )
            Spacer(Modifier.height(Spacing.md))
            KeyValueRow(
                stringResource(R.string.reports_linked_incidents),
                entry.incidents.toString(),
                valueColor = if (entry.incidents > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }

        is ReportEntry.Incident -> {
            KeyValueRow(
                stringResource(R.string.reports_severity),
                stringResource(R.string.reports_severity_value, entry.incident.severityLevel, stringResource(entry.incident.severity.labelRes))
            )
            DetailBlock(stringResource(R.string.reports_description), entry.incident.description)
            Photos(entry.incident.photoUris)
        }
    }
}

@Composable
private fun DetailBlock(label: String, text: String) {
    Spacer(Modifier.height(Spacing.md))
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(Spacing.xxs))
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun Photos(uris: List<String>) {
    if (uris.isEmpty()) return
    Spacer(Modifier.height(Spacing.md))
    PhotoAttachmentRow(
        photos = uris.mapIndexed { i, uri -> PhotoAttachment("$i", uri) },
        onAddPhoto = {},
        onRemovePhoto = null,
        maxPhotos = uris.size,
        enabled = false
    )
}

private val ReportType.labelRes: Int
    get() = when (this) {
        ReportType.DAILY -> R.string.reports_type_daily
        ReportType.INSPECTION -> R.string.reports_type_inspection
        ReportType.INCIDENT -> R.string.reports_type_incident
    }

private val ReportType.icon: ImageVector
    get() = when (this) {
        ReportType.DAILY -> Icons.Filled.Description
        ReportType.INSPECTION -> Icons.Filled.VerifiedUser
        ReportType.INCIDENT -> Icons.Filled.ReportProblem
    }

private val ReportEntry.tone: Tone
    get() = when (this) {
        is ReportEntry.Daily -> report.status.tone
        is ReportEntry.Inspection -> if (inspection.passed) Tone.Success else Tone.Danger
        is ReportEntry.Incident -> incident.severity.tone
    }

@Composable
private fun ReportEntry.title(): String = when (this) {
    is ReportEntry.Daily -> stringResource(R.string.reports_daily_title, report.date.formatDate())
    is ReportEntry.Inspection -> stringResource(
        if (inspection.passed) R.string.reports_inspection_passed else R.string.reports_inspection_failed
    )
    is ReportEntry.Incident -> incident.description
}

@Composable
private fun ReportEntry.statusText(): String = when (this) {
    is ReportEntry.Daily -> stringResource(report.status.labelRes)
    is ReportEntry.Inspection -> stringResource(if (inspection.passed) R.string.reports_passed else R.string.reports_failed)
    is ReportEntry.Incident -> stringResource(incident.severity.labelRes)
}

private fun previewState(): ReportsUiState {
    val today = LocalDate.now()
    val now = LocalDateTime.now()
    val riverside = Project(1, 2, "Riverside Estate Phase 2", "Centurion", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0)
    return ReportsUiState(
        entries = listOf(
            ReportEntry.Incident(IncidentReport(1, 4, 3, "Loose scaffold board on level 2, east side. Area taped off.", now.minusHours(2)), 1, riverside.projectName, "Thabo Mokoena"),
            ReportEntry.Daily(
                DailyReport(1, 1, 5, today.minusDays(1), "Sunny", "Blockwork to ground floor, units 3 and 4. Lintels placed.", "Cement delivery arrived two hours late.", ReportStatus.SUBMITTED),
                riverside.projectName, "Sipho Dlamini"
            ),
            ReportEntry.Inspection(SafetyInspection(4, 1, 6, today, "Failed: harness", false), riverside.projectName, "Thabo Mokoena", 1),
            ReportEntry.Daily(
                DailyReport(2, 1, 6, today.minusDays(2), "Cloudy", "Scaffold erected on the east elevation.", "", ReportStatus.APPROVED),
                riverside.projectName, "Thabo Mokoena"
            )
        ),
        projects = listOf(riverside),
        canApprove = true,
        expanded = setOf("daily-1"),
        now = now
    )
}

@DevicePreviews
@Composable
private fun ReportsPreview() {
    PreviewSurface {
        ReportsScreen(previewState(), twoPane = LocalWindowLayout.current.isExpanded, actions = ReportsActions())
    }
}
