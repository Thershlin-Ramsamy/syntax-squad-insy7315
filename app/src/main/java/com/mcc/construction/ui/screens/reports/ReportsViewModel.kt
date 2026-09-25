package com.mcc.construction.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.DailyReportRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.SafetyRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The three kinds of record in the reports history. */
enum class ReportType { DAILY, INSPECTION, INCIDENT }

/** One row in the reports history. */
sealed interface ReportEntry {
    /** Stable across types, e.g. "daily-4". */
    val key: String
    val type: ReportType
    val projectId: Int
    val projectName: String
    /** Who filed it. */
    val authorName: String
    /** When it was filed — what the list is sorted by. */
    val time: LocalDateTime

    data class Daily(
        val report: DailyReport,
        override val projectName: String,
        override val authorName: String
    ) : ReportEntry {
        override val key get() = "daily-${report.reportId}"
        override val type get() = ReportType.DAILY
        override val projectId get() = report.projectId
        override val time: LocalDateTime get() = report.date.atTime(END_OF_SHIFT)
    }

    data class Inspection(
        val inspection: SafetyInspection,
        override val projectName: String,
        override val authorName: String,
        val incidents: Int
    ) : ReportEntry {
        override val key get() = "inspection-${inspection.inspectionId}"
        override val type get() = ReportType.INSPECTION
        override val projectId get() = inspection.projectId
        override val time: LocalDateTime get() = inspection.date.atTime(START_OF_SHIFT)
    }

    data class Incident(
        val incident: IncidentReport,
        override val projectId: Int,
        override val projectName: String,
        override val authorName: String
    ) : ReportEntry {
        override val key get() = "incident-${incident.incidentId}"
        override val type get() = ReportType.INCIDENT
        override val time: LocalDateTime get() = incident.dateReported
    }

    private companion object {
        val END_OF_SHIFT: LocalTime = LocalTime.of(17, 0)
        val START_OF_SHIFT: LocalTime = LocalTime.of(7, 0)
    }
}

data class ReportsUiState(
    /** Null until the first load. Newest first. */
    val entries: List<ReportEntry>? = null,
    val projects: List<Project> = emptyList(),
    val projectFilter: Int? = null,
    val typeFilter: ReportType? = null,
    val awaitingOnly: Boolean = false,
    /** Managers and administrators can approve daily reports. */
    val canApprove: Boolean = false,
    val selectedKey: String? = null,
    /** Keys expanded in place on a phone. */
    val expanded: Set<String> = emptySet(),
    val approving: Set<Int> = emptySet(),
    val now: LocalDateTime = LocalDateTime.MIN,
    val message: UiMessage? = null
) {
    private val inProject: List<ReportEntry>
        get() = entries.orEmpty().filter { projectFilter == null || it.projectId == projectFilter }

    val visible: List<ReportEntry>
        get() = inProject.filter { entry ->
            (typeFilter == null || entry.type == typeFilter) &&
                (!awaitingOnly || entry is ReportEntry.Daily && entry.report.status == ReportStatus.SUBMITTED)
        }

    fun countFor(type: ReportType?): Int = inProject.count { type == null || it.type == type }

    val awaitingCount: Int
        get() = inProject.count { it is ReportEntry.Daily && it.report.status == ReportStatus.SUBMITTED }

    val paneKey: String?
        get() = selectedKey?.takeIf { key -> visible.any { it.key == key } } ?: visible.firstOrNull()?.key

    val paneEntry: ReportEntry? get() = visible.firstOrNull { it.key == paneKey }
}

/** Daily reports, safety inspections and incidents in one history, with report approval. */
class ReportsViewModel(
    authRepository: AuthRepository,
    private val dailyReportRepository: DailyReportRepository,
    safetyRepository: SafetyRepository,
    projectRepository: ProjectRepository,
    userRepository: UserRepository,
    private val clock: Clock
) : ViewModel() {

    private val filters = MutableStateFlow(Filters())
    private val transient = MutableStateFlow(Transient())

    private val entries = combine(
        dailyReportRepository.observeReports(),
        safetyRepository.observeInspections(),
        safetyRepository.observeIncidents(),
        projectRepository.observeProjects(),
        userRepository.observeUsers()
    ) { reports, inspections, incidents, projects, users ->
        val projectNames = projects.associate { it.projectId to it.projectName }
        val names = users.associate { it.userId to it.fullName }
        val inspectionsById = inspections.associateBy { it.inspectionId }
        val incidentCounts = incidents.groupingBy { it.inspectionId }.eachCount()
        val all = reports.map { ReportEntry.Daily(it, projectNames[it.projectId].orEmpty(), names[it.userId].orEmpty()) } +
            inspections.map {
                ReportEntry.Inspection(it, projectNames[it.projectId].orEmpty(), names[it.inspectorId].orEmpty(), incidentCounts[it.inspectionId] ?: 0)
            } +
            incidents.map { incident ->
                val inspection = inspectionsById[incident.inspectionId]
                ReportEntry.Incident(
                    incident = incident,
                    projectId = inspection?.projectId ?: -1,
                    projectName = inspection?.let { projectNames[it.projectId] }.orEmpty(),
                    authorName = inspection?.let { names[it.inspectorId] }.orEmpty()
                )
            }
        all.sortedByDescending { it.time } to projects.filter { p -> all.any { it.projectId == p.projectId } }
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        entries,
        filters,
        transient,
        authRepository.session
    ) { (list, projects), f, t, session ->
        ReportsUiState(
            entries = list,
            projects = projects,
            projectFilter = f.projectId,
            typeFilter = f.type,
            awaitingOnly = f.awaitingOnly,
            canApprove = session != null && session.role != UserRole.WORKER,
            selectedKey = f.selected,
            expanded = f.expanded,
            approving = t.approving,
            now = LocalDateTime.now(clock),
            message = t.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ReportsUiState())

    fun onProjectFilterChange(projectId: Int?) = filters.update { it.copy(projectId = projectId) }

    fun onTypeFilterChange(type: ReportType?) = filters.update { it.copy(type = type) }

    fun onAwaitingOnlyChange(value: Boolean) = filters.update { it.copy(awaitingOnly = value) }

    fun onSelect(key: String) = filters.update { it.copy(selected = key) }

    fun onToggleExpanded(key: String) = filters.update {
        it.copy(expanded = if (key in it.expanded) it.expanded - key else it.expanded + key)
    }

    fun onApprove(reportId: Int) {
        if (reportId in transient.value.approving) return
        val entry = uiState.value.entries.orEmpty()
            .filterIsInstance<ReportEntry.Daily>()
            .firstOrNull { it.report.reportId == reportId } ?: return
        transient.update { it.copy(approving = it.approving + reportId) }
        viewModelScope.launch {
            dailyReportRepository.approveReport(reportId)
            transient.update {
                it.copy(
                    approving = it.approving - reportId,
                    message = UiMessage(R.string.reports_approved_message, listOf(entry.authorName.substringBefore(' ')))
                )
            }
        }
    }

    fun onMessageShown() = transient.update { it.copy(message = null) }

    private data class Filters(
        val projectId: Int? = null,
        val type: ReportType? = null,
        val awaitingOnly: Boolean = false,
        val selected: String? = null,
        val expanded: Set<String> = emptySet()
    )

    private data class Transient(val approving: Set<Int> = emptySet(), val message: UiMessage? = null)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ReportsViewModel(
                    c.authRepository, c.dailyReportRepository, c.safetyRepository,
                    c.projectRepository, c.userRepository, c.clock
                )
            }
        }
    }
}
