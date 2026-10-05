package com.mcc.construction.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.*
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.launchSubmission
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val project: Project? = null,
    val assignees: List<User> = emptyList(),
    val photos: List<TaskPhoto> = emptyList(),
    val canUpdate: Boolean = false,
    val isUpdating: Boolean = false,
    val isCompleteWithoutPhotoDialogVisible: Boolean = false,
    val today: LocalDate = LocalDate.MIN,
    val message: UiMessage? = null
) {
    val isNotFound: Boolean get() = !isLoading && task == null

    val nextStatus: TaskStatus?
        get() = when (task?.status) {
            TaskStatus.PENDING -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            else -> null
        }

    val canAddPhotos: Boolean
        get() = canUpdate && task?.status != TaskStatus.COMPLETED
}

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
    private val taskAndProject =
        taskRepository.observeTask(taskId).flatMapLatest { task ->
            if (task == null) flowOf(null to null)
            else projectRepository.observeProject(task.projectId)
                .map { task to it }
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
            canUpdate = user != null && (
                    user.role != UserRole.WORKER ||
                            people.any { it.userId == user.userId }
                    ),
            isUpdating = extra.isUpdating,
            isCompleteWithoutPhotoDialogVisible = extra.isDialogVisible,
            today = LocalDate.now(clock),
            message = extra.message
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        TaskDetailUiState()
    )

    fun onAdvanceStatus() {
        val state = uiState.value
        val next = state.nextStatus ?: return

        if (!state.canUpdate || transient.value.isUpdating) return

        if (next == TaskStatus.COMPLETED && state.photos.isEmpty()) {
            transient.update { it.copy(isDialogVisible = true) }
            return
        }

        updateStatus(next)
    }

    fun onCompleteWithoutPhoto() {
        val state = uiState.value

        if (
            !state.canUpdate ||
            state.nextStatus != TaskStatus.COMPLETED ||
            transient.value.isUpdating
        ) return

        transient.update { it.copy(isDialogVisible = false) }
        updateStatus(TaskStatus.COMPLETED)
    }

    fun onDismissCompleteDialog() {
        transient.update { it.copy(isDialogVisible = false) }
    }

    fun onPhotosPicked(uris: List<String>) {
        val userId = authRepository.session.value?.user?.userId ?: return
        val state = uiState.value

        if (
            uris.isEmpty() ||
            !state.canAddPhotos ||
            transient.value.isUpdating
        ) return

        val room = (MAX_PHOTOS - state.photos.size).coerceAtLeast(0)
        val selected = uris.distinct().take(room)

        if (selected.isEmpty()) {
            transient.update {
                it.copy(
                    message = UiMessage(
                        R.string.task_photo_limit,
                        listOf(MAX_PHOTOS),
                        isError = true
                    )
                )
            }
            return
        }

        launchSubmission(
            isBusy = { transient.value.isUpdating },
            setBusy = { busy ->
                transient.update {
                    it.copy(
                        isUpdating = busy,
                        isDialogVisible = false
                    )
                }
            },
            failureMessage =
                "The photo upload could not finish. Some photos may already " +
                        "have uploaded. Check the task photos before selecting " +
                        "the remaining photos again."
        ) {
            selected.forEach {
                taskRepository.addProofPhoto(taskId, userId, it)
            }

            if (authRepository.session.value?.user?.userId == userId) {
                transient.update {
                    it.copy(
                        message = if (selected.size < uris.distinct().size) {
                            UiMessage(
                                R.string.task_photo_limit,
                                listOf(MAX_PHOTOS),
                                isError = true
                            )
                        } else {
                            UiMessage(
                                R.string.task_photos_added,
                                listOf(selected.size)
                            )
                        }
                    )
                }
            }
        }
    }

    fun onRemovePhoto(photoId: Int) {
        val state = uiState.value
        if (
            !state.canUpdate ||
            state.photos.none { it.photoId == photoId }
        ) return

        launchSubmission(
            isBusy = { transient.value.isUpdating },
            setBusy = { busy ->
                transient.update { it.copy(isUpdating = busy) }
            },
            failureMessage =
                "Photo removal could not be confirmed. Refresh the task " +
                        "and check the photo before trying again."
        ) {
            taskRepository.removeProofPhoto(photoId)
            transient.update {
                it.copy(
                    message = UiMessage(R.string.task_photo_removed)
                )
            }
        }
    }

    fun onMessageShown() {
        transient.update { it.copy(message = null) }
    }

    private fun updateStatus(status: TaskStatus) {
        if (!uiState.value.canUpdate) return

        launchSubmission(
            isBusy = { transient.value.isUpdating },
            setBusy = { busy ->
                transient.update { it.copy(isUpdating = busy) }
            },
            failureMessage =
                "The task update could not be confirmed. Check your connection " +
                        "and the latest task status. Another user may have changed " +
                        "the task."
        ) {
            taskRepository.updateStatus(taskId, status)

            transient.update {
                it.copy(
                    message = UiMessage(
                        if (status == TaskStatus.COMPLETED) {
                            R.string.task_completed_message
                        } else {
                            R.string.task_started_message
                        }
                    )
                )
            }
        }
    }

    private data class Transient(
        val isUpdating: Boolean = false,
        val isDialogVisible: Boolean = false,
        val message: UiMessage? = null
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
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