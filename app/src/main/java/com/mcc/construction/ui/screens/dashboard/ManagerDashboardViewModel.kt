package com.mcc.construction.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Notification
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.data.model.StockLevel
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.DailyReportRepository
import com.mcc.construction.data.repository.InventoryRepository
import com.mcc.construction.data.repository.NotificationRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.SafetyRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.screens.employees.AttendanceToday
import com.mcc.construction.ui.screens.employees.PresenceStatus
import com.mcc.construction.ui.screens.projects.ProjectSummary
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Something on site that needs the manager's attention. */
sealed interface DashboardAlert {
    val projectName: String

    /** A material at or below its minimum level. */
    data class LowStock(val item: Inventory, override val projectName: String) : DashboardAlert

    /** A safety checklist that failed, with no incident logged against it. */
    data class FailedInspection(
        val inspection: SafetyInspection,
        val inspectorName: String,
        override val projectName: String
    ) : DashboardAlert

    /** A recently reported incident. */
    data class Incident(val incident: IncidentReport, override val projectName: String) : DashboardAlert
}

/** Today's roll call across the workforce. */
data class AttendanceSummary(
    val onSite: Int = 0,
    val finished: Int = 0,
    val notIn: Int = 0,
    /** First names of people not yet clocked in, for a quick follow-up. */
    val notInNames: List<String> = emptyList()
) {
    val workforce: Int get() = onSite + finished + notIn
}

data class ManagerDashboardUiState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    val today: LocalDate = LocalDate.MIN,
    val now: LocalDateTime = LocalDateTime.MIN,
    /** Active and on-hold projects, soonest deadline first. */
    val projects: List<ProjectSummary> = emptyList(),
    val taskCounts: Map<TaskStatus, Int> = emptyMap(),
    val overdueTasks: Int = 0,
    val attendance: AttendanceSummary = AttendanceSummary(),
    /** Most serious first. */
    val alerts: List<DashboardAlert> = emptyList(),
    val reportsAwaitingApproval: Int = 0,
    /** The latest updates sent to this manager — task progress, reports, safety. */
    val updates: List<Notification> = emptyList(),
    /** Where "Assign a task" starts: the first active project. */
    val defaultProjectId: Int? = null
) {
    val openTasks: Int
        get() = taskCounts.filterKeys { it != TaskStatus.COMPLETED }.values.sum()
    val totalTasks: Int get() = taskCounts.values.sum()
}

/**
 * The manager's overview: projects and their progress, where work stands,
 * who is on site today and anything that needs attention.
 */
class ManagerDashboardViewModel(
    authRepository: AuthRepository,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    attendanceRepository: AttendanceRepository,
    userRepository: UserRepository,
    inventoryRepository: InventoryRepository,
    safetyRepository: SafetyRepository,
    dailyReportRepository: DailyReportRepository,
    notificationRepository: NotificationRepository,
    private val clock: Clock
) : ViewModel() {

    private val today = LocalDate.now(clock)

    private val work = combine(
        projectRepository.observeProjects(),
        taskRepository.observeTasks(),
        attendanceRepository.observeAttendanceOn(today),
        userRepository.observeUsers()
    ) { projects, tasks, attendance, users -> Work(projects, tasks, attendance, users) }

    private val safety = combine(
        inventoryRepository.observeInventory(),
        safetyRepository.observeInspections(),
        safetyRepository.observeIncidents(),
        dailyReportRepository.observeReports()
    ) { inventory, inspections, incidents, reports -> Safety(inventory, inspections, incidents, reports) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val updates = authRepository.session.flatMapLatest { session ->
        if (session == null) flowOf(emptyList()) else notificationRepository.observeNotifications(session.user.userId)
    }

    val uiState: StateFlow<ManagerDashboardUiState> = combine(
        authRepository.session,
        work,
        safety,
        updates
    ) { session, work, safety, updates ->
        val projectNames = work.projects.associate { it.projectId to it.projectName }
        val onTheBoard = work.projects
            .filter { it.status == ProjectStatus.ACTIVE || it.status == ProjectStatus.ON_HOLD }
            .sortedWith(compareBy({ it.status != ProjectStatus.ACTIVE }, { it.endDate }))

        ManagerDashboardUiState(
            isLoading = false,
            firstName = session?.user?.firstName.orEmpty(),
            today = today,
            now = LocalDateTime.now(clock),
            projects = onTheBoard.map { ProjectSummary.of(it, work.tasks) },
            taskCounts = TaskStatus.entries.associateWith { status -> work.tasks.count { it.status == status } },
            overdueTasks = work.tasks.count { it.status != TaskStatus.COMPLETED && it.deadline.isBefore(today) },
            attendance = rollCall(work),
            alerts = alerts(safety, work, projectNames),
            reportsAwaitingApproval = safety.reports.count { it.status == ReportStatus.SUBMITTED },
            updates = updates.take(UPDATES_SHOWN),
            defaultProjectId = onTheBoard.firstOrNull { it.status == ProjectStatus.ACTIVE }?.projectId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ManagerDashboardUiState())

    private fun rollCall(work: Work): AttendanceSummary {
        val workers = work.users.filter { it.role == UserRole.WORKER && it.isActive }
        val shifts = work.attendance.groupBy { it.userId }
        val statuses = workers.associateWith { AttendanceToday.from(shifts[it.userId].orEmpty()).status }
        return AttendanceSummary(
            onSite = statuses.count { it.value == PresenceStatus.ON_SITE },
            finished = statuses.count { it.value == PresenceStatus.FINISHED },
            notIn = statuses.count { it.value == PresenceStatus.NOT_IN },
            notInNames = statuses.filterValues { it == PresenceStatus.NOT_IN }.keys.map { it.firstName }
        )
    }

    private fun alerts(safety: Safety, work: Work, projectNames: Map<Int, String>): List<DashboardAlert> {
        val since = today.minusDays(ALERT_WINDOW_DAYS)
        val names = work.users.associate { it.userId to it.fullName }
        val inspectionProject = safety.inspections.associate { it.inspectionId to it.projectId }
        val withIncident = safety.incidents.map { it.inspectionId }.toSet()

        val incidents = safety.incidents
            .filter { !it.dateReported.toLocalDate().isBefore(since) }
            .sortedWith(compareByDescending<IncidentReport> { it.severityLevel }.thenByDescending { it.dateReported })
            .map { DashboardAlert.Incident(it, projectNames[inspectionProject[it.inspectionId]].orEmpty()) }
        val failed = safety.inspections
            .filter { !it.passed && !it.date.isBefore(since) && it.inspectionId !in withIncident }
            .sortedByDescending { it.date }
            .map { DashboardAlert.FailedInspection(it, names[it.inspectorId].orEmpty(), projectNames[it.projectId].orEmpty()) }
        val lowStock = safety.inventory
            .filter { it.stockLevel != StockLevel.IN_STOCK }
            .sortedWith(compareByDescending<Inventory> { it.stockLevel }.thenBy { it.materialName })
            .map { DashboardAlert.LowStock(it, projectNames[it.projectId].orEmpty()) }
        return incidents + failed + lowStock
    }

    private data class Work(
        val projects: List<Project>,
        val tasks: List<Task>,
        val attendance: List<Attendance>,
        val users: List<User>
    )

    private data class Safety(
        val inventory: List<Inventory>,
        val inspections: List<SafetyInspection>,
        val incidents: List<IncidentReport>,
        val reports: List<DailyReport>
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val ALERT_WINDOW_DAYS = 7L
        private const val UPDATES_SHOWN = 5

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ManagerDashboardViewModel(
                    c.authRepository, c.projectRepository, c.taskRepository, c.attendanceRepository,
                    c.userRepository, c.inventoryRepository, c.safetyRepository, c.dailyReportRepository,
                    c.notificationRepository, c.clock
                )
            }
        }
    }
}
