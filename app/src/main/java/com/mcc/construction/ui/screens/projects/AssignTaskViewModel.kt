package com.mcc.construction.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.TaskDraft
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.screens.employees.AttendanceToday
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A form field that still needs filling in. */
enum class AssignField { TITLE, DEADLINE, WORKERS }

/** Someone who can be given the task, with how busy they are. */
data class WorkerOption(val user: User, val today: AttendanceToday, val openTasks: Int)

/** The saved task and who was told about it. */
data class AssignResult(val task: Task, val workerNames: List<String>)

data class AssignTaskForm(
    val projectId: Int? = null,
    val title: String = "",
    val description: String = "",
    val deadline: LocalDate? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val workerIds: Set<Int> = emptySet(),
    val missing: Set<AssignField> = emptySet(),
    val isSaving: Boolean = false,
    val saved: AssignResult? = null
)

data class AssignTaskUiState(
    val isLoading: Boolean = true,
    /** Projects that can take new work. */
    val projects: List<Project> = emptyList(),
    val workers: List<WorkerOption> = emptyList(),
    val form: AssignTaskForm = AssignTaskForm(),
    val today: LocalDate = LocalDate.MIN,
    val message: UiMessage? = null
) {
    val selectedProject: Project? get() = projects.firstOrNull { it.projectId == form.projectId }
}

/**
 * Creates a task on a project and assigns it to one or more workers. Each
 * worker is sent a notification, so the task appears on their phone straight
 * away.
 */
class AssignTaskViewModel(
    initialProjectId: Int,
    projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    userRepository: UserRepository,
    attendanceRepository: AttendanceRepository,
    private val clock: Clock
) : ViewModel() {

    private val today = LocalDate.now(clock)
    private val form = MutableStateFlow(AssignTaskForm(projectId = initialProjectId))
    private val message = MutableStateFlow<UiMessage?>(null)

    private val options = combine(
        projectRepository.observeProjects(),
        userRepository.observeUsersWithRole(UserRole.WORKER),
        attendanceRepository.observeAttendanceOn(today),
        taskRepository.observeTasks(),
        taskRepository.observeAllAssignments()
    ) { projects, workers, attendance, tasks, assignments ->
        val open = tasks.filter { it.status != TaskStatus.COMPLETED }.map { it.taskId }.toSet()
        val shifts = attendance.groupBy { it.userId }
        Options(
            projects = projects.filter { it.status != ProjectStatus.COMPLETED },
            workers = workers.filter { it.isActive }.sortedBy { it.fullName }.map { user ->
                WorkerOption(
                    user = user,
                    today = AttendanceToday.from(shifts[user.userId].orEmpty()),
                    openTasks = assignments.count { it.userId == user.userId && it.taskId in open }
                )
            }
        )
    }

    val uiState: StateFlow<AssignTaskUiState> = combine(options, form, message) { options, form, message ->
        AssignTaskUiState(
            isLoading = false,
            projects = options.projects,
            workers = options.workers,
            form = form,
            today = today,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AssignTaskUiState())

    fun onProjectSelected(projectId: Int) = edit { it.copy(projectId = projectId) }

    fun onTitleChange(value: String) = edit { it.copy(title = value, missing = it.missing - AssignField.TITLE) }

    fun onDescriptionChange(value: String) = edit { it.copy(description = value) }

    fun onDeadlineSelected(date: LocalDate) = edit { it.copy(deadline = date, missing = it.missing - AssignField.DEADLINE) }

    fun onPrioritySelected(priority: TaskPriority) = edit { it.copy(priority = priority) }

    fun onToggleWorker(userId: Int) = edit {
        val ids = if (userId in it.workerIds) it.workerIds - userId else it.workerIds + userId
        it.copy(workerIds = ids, missing = it.missing - AssignField.WORKERS)
    }

    fun onSave() {
        val current = form.value
        if (current.isSaving || current.saved != null) return
        val missing = buildSet {
            if (current.title.isBlank()) add(AssignField.TITLE)
            if (current.deadline == null) add(AssignField.DEADLINE)
            if (current.workerIds.isEmpty()) add(AssignField.WORKERS)
        }
        val projectId = current.projectId
        val deadline = current.deadline
        if (missing.isNotEmpty() || projectId == null || deadline == null) {
            form.update { it.copy(missing = missing) }
            message.value = UiMessage(R.string.assign_missing_fields, isError = true)
            return
        }
        if (deadline.isBefore(today)) {
            message.value = UiMessage(R.string.assign_deadline_past, isError = true)
            return
        }
        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val task = taskRepository.createTask(
                TaskDraft(
                    projectId = projectId,
                    title = current.title.trim(),
                    description = current.description.trim(),
                    deadline = deadline,
                    priority = current.priority
                )
            )
            val workers = uiState.value.workers.filter { it.user.userId in current.workerIds }
            workers.forEach { taskRepository.assignTask(task.taskId, it.user.userId) }
            form.update { it.copy(isSaving = false, saved = AssignResult(task, workers.map { w -> w.user.firstName })) }
        }
    }

    /** Clears the form for another task on the same project. */
    fun onAssignAnother() {
        form.update { AssignTaskForm(projectId = it.projectId) }
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun edit(change: (AssignTaskForm) -> AssignTaskForm) {
        form.update { if (it.isSaving || it.saved != null) it else change(it) }
    }

    private data class Options(val projects: List<Project>, val workers: List<WorkerOption>)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(projectId: Int) = viewModelFactory {
            initializer {
                val c = appContainer()
                AssignTaskViewModel(
                    projectId, c.projectRepository, c.taskRepository, c.userRepository, c.attendanceRepository, c.clock
                )
            }
        }
    }
}
