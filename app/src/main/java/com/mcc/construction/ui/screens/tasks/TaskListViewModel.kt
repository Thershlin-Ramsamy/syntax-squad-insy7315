package com.mcc.construction.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.ui.common.appContainer
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** The status filter on the task list. [ALL] also shows blocked work. */
enum class TaskFilter(val status: TaskStatus?) {
    ALL(null),
    PENDING(TaskStatus.PENDING),
    IN_PROGRESS(TaskStatus.IN_PROGRESS),
    COMPLETED(TaskStatus.COMPLETED)
}

data class TaskListUiState(
    /** Null until the first load, so the screen can tell loading from empty. */
    val items: List<TaskListItem>? = null,
    val filter: TaskFilter = TaskFilter.ALL,
    val today: LocalDate = LocalDate.MIN,
    /** Managers and administrators see every task; workers see their own. */
    val showsAllTasks: Boolean = false,
    /** The task open in the side-by-side detail pane on wide windows. */
    val selectedTaskId: Int? = null
) {
    fun countFor(filter: TaskFilter): Int =
        items.orEmpty().count { filter.status == null || it.task.status == filter.status }

    val visible: List<TaskListItem>
        get() = items.orEmpty().filter { filter.status == null || it.task.status == filter.status }

    /**
     * Today's work: open tasks due today or earlier, anything under way, and
     * tasks due today that are already done.
     */
    val dueNow: List<TaskListItem>
        get() = visible.filter {
            val task = it.task
            when (task.status) {
                TaskStatus.IN_PROGRESS -> true
                TaskStatus.COMPLETED -> task.deadline == today
                else -> !task.deadline.isAfter(today)
            }
        }

    /** Open work due after today. */
    val comingUp: List<TaskListItem>
        get() = visible.filter { it.task.status != TaskStatus.COMPLETED } - dueNow.toSet()

    /** Finished work that was due before today — kept out of today's list. */
    val doneEarlier: List<TaskListItem>
        get() = visible.filter { it.task.status == TaskStatus.COMPLETED } - dueNow.toSet()

    /** What the detail pane shows: the chosen task, or the first in the list. */
    val paneTaskId: Int?
        get() = selectedTaskId?.takeIf { id -> visible.any { it.task.taskId == id } }
            ?: visible.firstOrNull()?.task?.taskId
}

class TaskListViewModel(
    authRepository: AuthRepository,
    taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
    private val clock: Clock
) : ViewModel() {

    private val filter = MutableStateFlow(TaskFilter.ALL)
    private val selectedTaskId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val items = authRepository.session.flatMapLatest { session ->
        when {
            session == null -> flowOf(null)
            session.role == UserRole.WORKER -> taskRepository.observeTasksAssignedTo(session.user.userId)
            else -> taskRepository.observeTasks()
        }
    }

    val uiState: StateFlow<TaskListUiState> = combine(
        items,
        projectRepository.observeProjects(),
        authRepository.session,
        filter,
        selectedTaskId
    ) { tasks, projects, session, selectedFilter, selected ->
        val names = projects.associate { it.projectId to it.projectName }
        TaskListUiState(
            items = tasks?.sortedWith(TaskWorkOrder)?.map { TaskListItem(it, names[it.projectId].orEmpty()) },
            filter = selectedFilter,
            today = LocalDate.now(clock),
            showsAllTasks = session != null && session.role != UserRole.WORKER,
            selectedTaskId = selected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TaskListUiState())

    fun onFilterChange(selected: TaskFilter) {
        filter.update { selected }
    }

    fun onSelectTask(taskId: Int) {
        selectedTaskId.update { taskId }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                TaskListViewModel(c.authRepository, c.taskRepository, c.projectRepository, c.clock)
            }
        }
    }
}
