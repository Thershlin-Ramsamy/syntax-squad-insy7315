package com.mcc.construction.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPhoto
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.UiMessage
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val project: Project? = null,
    val assignees: List<User> = emptyList(),
    val photos: List<TaskPhoto> = emptyList(),
    /** The signed-in user may move this task on: they are assigned to it, or they manage. */
    val canUpdate: Boolean = false,
    val isUpdating: Boolean = false,
    val isCompleteWithoutPhotoDialogVisible: Boolean = false,
    val today: LocalDate = LocalDate.MIN,
    val message: UiMessage? = null
) {
    val isNotFound: Boolean get() = !isLoading && task == null

    /** Where the main button takes the task next, or null when there is no next step. */
    val nextStatus: TaskStatus?
        get() = when (task?.status) {
            TaskStatus.PENDING -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            else -> null
        }

    val canAddPhotos: Boolean get() = canUpdate && task?.status != TaskStatus.COMPLETED
}

/**
 * One task: its details, who is on it, proof-of-work photos, and the step
 * from Pending to In progress to Completed. Every change goes through the
 * TaskRepository, so the dashboard, the task list and a manager's view all
 * update at once.
 */
class TaskDetailViewModel(
    private val taskId: Int,
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
    userRepository: UserRepository,
    private val syncRepository: SyncRepository,
    private val clock: Clock
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val taskAndProject = taskRepository.observeTask(taskId).flatMapLatest { task ->
        if (task == null) flowOf(null to null)
        else projectRepository.observeProject(task.projectId).map { task to it }
    }

    private val assignees = combine(
        taskRepository.observeAssignments(taskId),
        userRepository.observeUsers()
    ) { assignments, users ->
        val ids = assignments.map { it.userId }.toSet()
        users.filter { it.userId in ids }
    }

    val uiState: StateFlow<TaskDetailUiState> = combine(
        taskAndProject,
        assignees,
        taskRepository.observeProofPhotos(taskId),
        authRepository.session,
        transient
    ) { (task, project), people, photos, session, extra ->
        val user = session?.user
        TaskDetailUiState(
            isLoading = false,
            task = task,
            project = project,
            assignees = people,
            photos = photos,
            canUpdate = user != null && (user.role != UserRole.WORKER || people.any { it.userId == user.userId }),
            isUpdating = extra.isUpdating,
            isCompleteWithoutPhotoDialogVisible = extra.isDialogVisible,
            today = LocalDate.now(clock),
            message = extra.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TaskDetailUiState())

    /**
     * Moves the task one step on. Completing with no photo asks first, since a
     * photo is what lets the foreman sign the work off without a site visit.
     */
    fun onAdvanceStatus() {
        val state = uiState.value
        val next = state.nextStatus ?: return
        if (!state.canUpdate || state.isUpdating) return
        if (next == TaskStatus.COMPLETED && state.photos.isEmpty()) {
            transient.update { it.copy(isDialogVisible = true) }
            return
        }
        updateStatus(next)
    }

    fun onCompleteWithoutPhoto() {
        transient.update { it.copy(isDialogVisible = false) }
        updateStatus(TaskStatus.COMPLETED)
    }

    fun onDismissCompleteDialog() {
        transient.update { it.copy(isDialogVisible = false) }
    }

    fun onPhotosPicked(uris: List<String>) {
        val userId = authRepository.session.value?.user?.userId ?: return
        if (uris.isEmpty() || !uiState.value.canAddPhotos) return
        transient.update { it.copy(isDialogVisible = false) }
        viewModelScope.launch {
            val room = MAX_PHOTOS - uiState.value.photos.size
            val added = uris.take(room.coerceAtLeast(0))
            added.forEach { taskRepository.addProofPhoto(taskId, userId, it) }
            val message = if (added.size < uris.size) {
                UiMessage(R.string.task_photo_limit, listOf(MAX_PHOTOS), isError = true)
            } else {
                UiMessage(R.string.task_photos_added, listOf(added.size))
            }
            transient.update { it.copy(message = message) }
        }
    }

    fun onRemovePhoto(photoId: Int) {
        viewModelScope.launch {
            taskRepository.removeProofPhoto(photoId)
            transient.update { it.copy(message = UiMessage(R.string.task_photo_removed)) }
        }
    }

    fun onMessageShown() {
        transient.update { it.copy(message = null) }
    }

    private fun updateStatus(status: TaskStatus) {
        transient.update { it.copy(isUpdating = true) }
        viewModelScope.launch {
            taskRepository.updateStatus(taskId, status)
            val online = syncRepository.status.value.isOnline
            val textRes = when {
                status == TaskStatus.COMPLETED && online -> R.string.task_completed_message
                status == TaskStatus.COMPLETED -> R.string.task_completed_offline_message
                online -> R.string.task_started_message
                else -> R.string.task_started_offline_message
            }
            transient.update { it.copy(isUpdating = false, message = UiMessage(textRes)) }
        }
    }

    private data class Transient(
        val isUpdating: Boolean = false,
        val isDialogVisible: Boolean = false,
        val message: UiMessage? = null
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Proof photos per task; enough to show the work without filling the device. */
        const val MAX_PHOTOS = 5

        fun factory(taskId: Int) = viewModelFactory {
            initializer {
                val c = appContainer()
                TaskDetailViewModel(
                    taskId,
                    c.authRepository,
                    c.taskRepository,
                    c.projectRepository,
                    c.userRepository,
                    c.syncRepository,
                    c.clock
                )
            }
        }
    }
}
