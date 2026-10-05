package com.mcc.construction.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.*
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.launchSubmission
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.*

enum class ReportType {
    DAILY, INSPECTION, INCIDENT
}

sealed interface ReportEntry {
    val key: String
    val type: ReportType
    val projectId: Int
    val projectName: String
    val authorName: String
    val time: LocalDateTime

    data class Daily(
        val report: DailyReport,
        override val projectName: String,
        override val authorName: String
    ) : ReportEntry {
        override val key get() = "daily-${report.reportId}"
        override val type get() = ReportType.DAILY
        override val projectId get() = report.projectId
        override val time get() = report.date.atTime(LocalTime.of(17, 0))
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
        override val time get() = inspection.date.atTime(LocalTime.of(7, 0))
    }

    data class Incident(
        val incident: IncidentReport,
        override val projectId: Int,
        override val projectName: String,
        override val authorName: String
    ) : ReportEntry {
        override val key get() = "incident-${incident.incidentId}"
        override val type get() = ReportType.INCIDENT
        override val time get() = incident.dateReported
    }
}

data class ReportsUiState(
    val entries: List<ReportEntry>? = null,
    val projects: List<Project> = emptyList(),
    val projectFilter: Int? = null,
    val typeFilter: ReportType? = null,
    val awaitingOnly: Boolean = false,
    val canApprove: Boolean = false,
    val selectedKey: String? = null,
    val expanded: Set<String> = emptySet(),
    val approving: Set<Int> = emptySet(),
    val now: LocalDateTime = LocalDateTime.MIN,
    val message: UiMessage? = null
) {
    private val inProject: List<ReportEntry>
        get() = entries.orEmpty().filter {
            projectFilter == null || it.projectId == projectFilter
        }

    val visible: List<ReportEntry>
        get() = inProject.filter {
            (typeFilter == null || it.type == typeFilter) &&
                    (
                            !awaitingOnly ||
                                    it is ReportEntry.Daily &&
                                    it.report.status == ReportStatus.SUBMITTED
                            )
        }

    fun countFor(type: ReportType?): Int =
        inProject.count { type == null || it.type == type }

    val awaitingCount: Int
        get() = inProject.count {
            it is ReportEntry.Daily &&
                    it.report.status == ReportStatus.SUBMITTED
        }

    val paneKey: String?
        get() = selectedKey?.takeIf { key ->
            visible.any { it.key == key }
        } ?: visible.firstOrNull()?.key

    val paneEntry: ReportEntry?
        get() = visible.firstOrNull { it.key == paneKey }
}

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
        val projectNames = projects.associate {
            it.projectId to it.projectName
        }
        val names = users.associate { it.userId to it.fullName }
        val inspectionsById = inspections.associateBy { it.inspectionId }
        val incidentCounts = incidents.groupingBy { it.inspectionId }
            .eachCount()

        val all: List<ReportEntry> =
            reports.map {
                ReportEntry.Daily(
                    it,
                    projectNames[it.projectId].orEmpty(),
                    names[it.userId].orEmpty()
                )
            } + inspections.map {
                ReportEntry.Inspection(
                    it,
                    projectNames[it.projectId].orEmpty(),
                    names[it.inspectorId].orEmpty(),
                    incidentCounts[it.inspectionId] ?: 0
                )
            } + incidents.map { incident ->
                val inspection = inspectionsById[incident.inspectionId]
                ReportEntry.Incident(
                    incident,
                    inspection?.projectId ?: -1,
                    inspection?.let {
                        projectNames[it.projectId]
                    }.orEmpty(),
                    inspection?.let {
                        names[it.inspectorId]
                    }.orEmpty()
                )
            }

        all.sortedByDescending { it.time } to projects.filter { project ->
            all.any { it.projectId == project.projectId }
        }
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        entries,
        filters,
        transient,
        authRepository.session
    ) { (list, projects), filter, extra, session ->
        ReportsUiState(
            entries = list,
            projects = projects,
            projectFilter = filter.projectId,
            typeFilter = filter.type,
            awaitingOnly = filter.awaitingOnly,
            canApprove = session != null && session.role != UserRole.WORKER,
            selectedKey = filter.selected,
            expanded = filter.expanded,
            approving = extra.approving,
            now = LocalDateTime.now(clock),
            message = extra.message
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        ReportsUiState()
    )

    fun onProjectFilterChange(projectId: Int?) =
        filters.update { it.copy(projectId = projectId) }

    fun onTypeFilterChange(type: ReportType?) =
        filters.update { it.copy(type = type) }

    fun onAwaitingOnlyChange(value: Boolean) =
        filters.update { it.copy(awaitingOnly = value) }

    fun onSelect(key: String) =
        filters.update { it.copy(selected = key) }

    fun onToggleExpanded(key: String) = filters.update {
        it.copy(
            expanded = if (key in it.expanded) {
                it.expanded - key
            } else {
                it.expanded + key
            }
        )
    }

    fun onApprove(reportId: Int) {
        val state = uiState.value
        if (!state.canApprove) return

        val entry = state.entries.orEmpty()
            .filterIsInstance<ReportEntry.Daily>()
            .firstOrNull {
                it.report.reportId == reportId &&
                        it.report.status == ReportStatus.SUBMITTED
            } ?: return

        launchSubmission(
            isBusy = { reportId in transient.value.approving },
            setBusy = { busy ->
                transient.update {
                    it.copy(
                        approving = if (busy) {
                            it.approving + reportId
                        } else {
                            it.approving - reportId
                        }
                    )
                }
            },
            failureMessage =
                "Report approval could not be confirmed. Check your connection " +
                        "and the report status before trying again."
        ) {
            dailyReportRepository.approveReport(reportId)
            transient.update {
                it.copy(
                    message = UiMessage(
                        R.string.reports_approved_message,
                        listOf(entry.authorName.substringBefore(' '))
                    )
                )
            }
        }
    }

    fun onMessageShown() =
        transient.update { it.copy(message = null) }

    private data class Filters(
        val projectId: Int? = null,
        val type: ReportType? = null,
        val awaitingOnly: Boolean = false,
        val selected: String? = null,
        val expanded: Set<String> = emptySet()
    )

    private data class Transient(
        val approving: Set<Int> = emptySet(),
        val message: UiMessage? = null
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ReportsViewModel(
                    c.authRepository,
                    c.dailyReportRepository,
                    c.safetyRepository,
                    c.projectRepository,
                    c.userRepository,
                    c.clock
                )
            }
        }
    }
}