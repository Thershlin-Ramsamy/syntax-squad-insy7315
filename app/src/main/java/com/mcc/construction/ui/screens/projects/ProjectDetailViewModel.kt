package com.mcc.construction.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.DailyReportRepository
import com.mcc.construction.data.repository.DocumentRepository
import com.mcc.construction.data.repository.InventoryRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.screens.employees.AttendanceToday
import com.mcc.construction.ui.screens.tasks.TaskWorkOrder
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A task on the project with the people working on it. */
data class ProjectTask(val task: Task, val assignees: List<User>)

/** Someone on the project team and where they are today. */
data class TeamMember(val user: User, val today: AttendanceToday, val openTasks: Int)

/** A daily report with its author's name. */
data class ProjectReport(val report: DailyReport, val authorName: String)

data class ProjectDetailUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val managerName: String = "",
    val tasks: List<ProjectTask> = emptyList(),
    val team: List<TeamMember> = emptyList(),
    val materials: List<Inventory> = emptyList(),
    val documents: List<Document> = emptyList(),
    val reports: List<ProjectReport> = emptyList(),
    val today: LocalDate = LocalDate.MIN
)

/** Everything about one project: overview, team, tasks, materials, documents and reports. */
class ProjectDetailViewModel(
    private val projectId: Int,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    userRepository: UserRepository,
    attendanceRepository: AttendanceRepository,
    inventoryRepository: InventoryRepository,
    documentRepository: DocumentRepository,
    dailyReportRepository: DailyReportRepository,
    private val clock: Clock
) : ViewModel() {

    private val today = LocalDate.now(clock)

    private val people = combine(
        taskRepository.observeTasksForProject(projectId),
        taskRepository.observeAllAssignments(),
        userRepository.observeUsers(),
        attendanceRepository.observeAttendanceOn(today)
    ) { tasks, assignments, users, attendance ->
        val byId = users.associateBy { it.userId }
        val taskIds = tasks.map { it.taskId }.toSet()
        val onProject = assignments.filter { it.taskId in taskIds }
        val projectTasks = tasks.sortedWith(TaskWorkOrder).map { task ->
            ProjectTask(task, onProject.filter { it.taskId == task.taskId }.mapNotNull { byId[it.userId] })
        }
        val shifts = attendance.groupBy { it.userId }
        val team = onProject.map { it.userId }.distinct().mapNotNull { byId[it] }
            .sortedBy { it.fullName }
            .map { user ->
                TeamMember(
                    user = user,
                    today = AttendanceToday.from(shifts[user.userId].orEmpty()),
                    openTasks = projectTasks.count { pt -> pt.task.status != TaskStatus.COMPLETED && pt.assignees.any { it.userId == user.userId } }
                )
            }
        People(projectTasks, team, byId)
    }

    val uiState: StateFlow<ProjectDetailUiState> = combine(
        projectRepository.observeProject(projectId),
        people,
        inventoryRepository.observeInventoryForProject(projectId),
        documentRepository.observeDocumentsForProject(projectId),
        dailyReportRepository.observeReports()
    ) { project, people, materials, documents, reports ->
        ProjectDetailUiState(
            isLoading = false,
            project = project,
            managerName = project?.let { people.users[it.managerId]?.fullName }.orEmpty(),
            tasks = people.tasks,
            team = people.team,
            materials = materials.sortedWith(compareByDescending<Inventory> { it.stockLevel }.thenBy { it.materialName }),
            documents = documents,
            reports = reports
                .filter { it.projectId == projectId }
                .take(RECENT_REPORTS)
                .map { ProjectReport(it, people.users[it.userId]?.fullName.orEmpty()) },
            today = today
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProjectDetailUiState())

    private data class People(val tasks: List<ProjectTask>, val team: List<TeamMember>, val users: Map<Int, User>)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val RECENT_REPORTS = 5

        fun factory(projectId: Int) = viewModelFactory {
            initializer { create(projectId) }
        }

        private fun CreationExtras.create(projectId: Int): ProjectDetailViewModel {
            val c = appContainer()
            return ProjectDetailViewModel(
                projectId, c.projectRepository, c.taskRepository, c.userRepository, c.attendanceRepository,
                c.inventoryRepository, c.documentRepository, c.dailyReportRepository, c.clock
            )
        }
    }
}
