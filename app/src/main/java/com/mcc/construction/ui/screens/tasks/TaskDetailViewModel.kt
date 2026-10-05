package com.mcc.construction.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.*
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.runBackendMutation
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val isNotFound: Boolean
        get() = !isLoading && task == null

    val nextStatus: TaskStatus?
        get() = when (task?.status) {
            TaskStatus.PENDING -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            else -> null
        }

    val canAddPhotos: Boolean
        get() = canUpdate &&
                task?.status != TaskStatus.COMPLETED
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
            if (task == null) {
                flowOf(null to null)
            } else {
                projectRepository.observeProject(task.projectId).map {
                    task to it
                }
            }
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

        if (!state.canUpdate || state.isUpdating) return

        if (
            next == TaskStatus.COMPLETED &&
            state.photos.isEmpty()
        ) {
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

        if (
            uris.isEmpty() ||
            !uiState.value.canAddPhotos ||
            transient.value.isUpdating
        ) return

        transient.update {
            it.copy(
                isDialogVisible = false,
                isUpdating = true
            )
        }

        viewModelScope.launch {
            runBackendMutation(
                onFinally = {
                    transient.update { it.copy(isUpdating = false) }
                }
            ) {
                val room = MAX_PHOTOS - uiState.value.photos.size
                val added = uris.take(room.coerceAtLeast(0))

                added.forEach {
                    taskRepository.addProofPhoto(taskId, userId, it)
                }

                val message = if (added.size < uris.size) {
                    UiMessage(
                        R.string.task_photo_limit,
                        listOf(MAX_PHOTOS),
                        isError = true
                    )
                } else {
                    UiMessage(
                        R.string.task_photos_added,
                        listOf(added.size)
                    )
                }

                transient.update { it.copy(message = message) }
            }
        }
    }

    fun onRemovePhoto(photoId: Int) {
        if (
            !uiState.value.canAddPhotos ||
            transient.value.isUpdating
        ) return

        transient.update { it.copy(isUpdating = true) }

        viewModelScope.launch {
            runBackendMutation(
                onFinally = {
                    transient.update { it.copy(isUpdating = false) }
                }
            ) {
                taskRepository.removeProofPhoto(photoId)

                transient.update {
                    it.copy(
                        message = UiMessage(R.string.task_photo_removed)
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        transient.update { it.copy(message = null) }
    }

    private fun updateStatus(status: TaskStatus) {
        if (
            !uiState.value.canUpdate ||
            transient.value.isUpdating
        ) return

        transient.update { it.copy(isUpdating = true) }

        viewModelScope.launch {
            runBackendMutation(
                onFinally = {
                    transient.update { it.copy(isUpdating = false) }
                }
            ) {
                taskRepository.updateStatus(taskId, status)
                val online = syncRepository.status.value.isOnline

                val textRes = when {
                    status == TaskStatus.COMPLETED && online ->
                        R.string.task_completed_message
                    status == TaskStatus.COMPLETED ->
                        R.string.task_completed_offline_message
                    online ->
                        R.string.task_started_message
                    else ->
                        R.string.task_started_offline_message
                }

                transient.update {
                    it.copy(
                        isUpdating = false,
                        message = UiMessage(textRes)
                    )
                }
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