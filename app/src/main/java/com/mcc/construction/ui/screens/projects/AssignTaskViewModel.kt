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
import com.mcc.construction.data.repository.AtomicTaskRepository
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AssignField {
    TITLE, DEADLINE, WORKERS
}

data class WorkerOption(
    val user: User,
    val today: AttendanceToday,
    val openTasks: Int
)

data class AssignResult(
    val task: Task,
    val workerNames: List<String>
)

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
    val projects: List<Project> = emptyList(),
    val workers: List<WorkerOption> = emptyList(),
    val form: AssignTaskForm = AssignTaskForm(),
    val today: LocalDate = LocalDate.MIN,
    val message: UiMessage? = null
) {
    val selectedProject: Project?
        get() = projects.firstOrNull {
            it.projectId == form.projectId
        }
}

class AssignTaskViewModel(
    initialProjectId: Int,
    projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    userRepository: UserRepository,
    attendanceRepository: AttendanceRepository,
    private val clock: Clock
) : ViewModel() {

    private val today = LocalDate.now(clock)

    private val form = MutableStateFlow(
        AssignTaskForm(projectId = initialProjectId)
    )

    private val message = MutableStateFlow<UiMessage?>(null)

    private val options = combine(
        projectRepository.observeProjects(),
        userRepository.observeUsersWithRole(UserRole.WORKER),
        attendanceRepository.observeAttendanceOn(today),
        taskRepository.observeTasks(),
        taskRepository.observeAllAssignments()
    ) { projects, workers, attendance, tasks, assignments ->
        val openTaskIds = tasks.filter {
            it.status != TaskStatus.COMPLETED
        }.map {
            it.taskId
        }.toSet()

        val shifts = attendance.groupBy {
            it.userId
        }

        Options(
            projects = projects.filter {
                it.status != ProjectStatus.COMPLETED
            },
            workers = workers.filter {
                it.isActive
            }.sortedBy {
                it.fullName
            }.map { user ->
                WorkerOption(
                    user = user,
                    today = AttendanceToday.from(
                        shifts[user.userId].orEmpty()
                    ),
                    openTasks = assignments.count {
                        it.userId == user.userId &&
                                it.taskId in openTaskIds
                    }
                )
            }
        )
    }

    val uiState: StateFlow<AssignTaskUiState> =
        combine(options, form, message) {
                available, currentForm, currentMessage ->
            AssignTaskUiState(
                isLoading = false,
                projects = available.projects,
                workers = available.workers,
                form = currentForm,
                today = today,
                message = currentMessage
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            AssignTaskUiState()
        )

    fun onProjectSelected(projectId: Int) =
        edit { it.copy(projectId = projectId) }

    fun onTitleChange(value: String) = edit {
        it.copy(
            title = value,
            missing = it.missing - AssignField.TITLE
        )
    }

    fun onDescriptionChange(value: String) =
        edit { it.copy(description = value) }

    fun onDeadlineSelected(date: LocalDate) = edit {
        it.copy(
            deadline = date,
            missing = it.missing - AssignField.DEADLINE
        )
    }

    fun onPrioritySelected(priority: TaskPriority) =
        edit { it.copy(priority = priority) }

    fun onToggleWorker(userId: Int) = edit {
        val selectedIds = if (userId in it.workerIds) {
            it.workerIds - userId
        } else {
            it.workerIds + userId
        }

        it.copy(
            workerIds = selectedIds,
            missing = it.missing - AssignField.WORKERS
        )
    }

    fun onSave() {
        val current = form.value

        if (current.isSaving || current.saved != null) {
            return
        }

        val missing = buildSet {
            if (current.title.isBlank()) {
                add(AssignField.TITLE)
            }

            if (current.deadline == null) {
                add(AssignField.DEADLINE)
            }

            if (current.workerIds.isEmpty()) {
                add(AssignField.WORKERS)
            }
        }

        val projectId = current.projectId
        val deadline = current.deadline

        if (
            missing.isNotEmpty() ||
            projectId == null ||
            deadline == null
        ) {
            form.update {
                it.copy(missing = missing)
            }

            message.value = UiMessage(
                R.string.assign_missing_fields,
                isError = true
            )

            return
        }

        if (deadline.isBefore(today)) {
            message.value = UiMessage(
                R.string.assign_deadline_past,
                isError = true
            )

            return
        }

        val selectedWorkers = uiState.value.workers.filter {
            it.user.userId in current.workerIds
        }

        if (
            selectedWorkers.size != current.workerIds.size ||
            uiState.value.projects.none {
                it.projectId == projectId
            }
        ) {
            message.value = UiMessage(
                R.string.assign_missing_fields,
                isError = true
            )
            return
        }

        form.update {
            it.copy(isSaving = true)
        }

        viewModelScope.launch {
            try {
                val draft = TaskDraft(
                    projectId = projectId,
                    title = current.title.trim(),
                    description = current.description.trim(),
                    deadline = deadline,
                    priority = current.priority
                )

                val workerIds = selectedWorkers.map {
                    it.user.userId
                }

                val repository = taskRepository

                val task = if (repository is AtomicTaskRepository) {
                    repository.createAssignedTask(
                        draft = draft,
                        workerIds = workerIds
                    )
                } else {
                    // Compatibility with the existing demo repository.
                    val created = repository.createTask(draft)

                    workerIds.forEach { workerId ->
                        repository.assignTask(
                            created.taskId,
                            workerId
                        )
                    }

                    created
                }

                form.update {
                    it.copy(
                        saved = AssignResult(
                            task = task,
                            workerNames = selectedWorkers.map { worker ->
                                worker.user.firstName
                            }
                        )
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                message.value = UiMessage(
                    R.string.backend_operation_failed,
                    isError = true
                )
            } finally {
                form.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    fun onAssignAnother() {
        form.update {
            AssignTaskForm(projectId = it.projectId)
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun edit(
        change: (AssignTaskForm) -> AssignTaskForm
    ) {
        form.update {
            if (it.isSaving || it.saved != null) {
                it
            } else {
                change(it)
            }
        }
    }

    private data class Options(
        val projects: List<Project>,
        val workers: List<WorkerOption>
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(projectId: Int) = viewModelFactory {
            initializer {
                val container = appContainer()

                AssignTaskViewModel(
                    initialProjectId = projectId,
                    projectRepository = container.projectRepository,
                    taskRepository = container.taskRepository,
                    userRepository = container.userRepository,
                    attendanceRepository = container.attendanceRepository,
                    clock = container.clock
                )
            }
        }
    }
}