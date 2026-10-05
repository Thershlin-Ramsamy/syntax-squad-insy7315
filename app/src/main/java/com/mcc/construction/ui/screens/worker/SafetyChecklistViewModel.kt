package com.mcc.construction.ui.screens.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.SafetyChecklistItem
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.SafetyRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.ui.common.BackendFeedback
import com.mcc.construction.ui.common.appContainer
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChecklistResult {
    INCOMPLETE,
    PASS,
    FAIL
}

data class ChecklistForm(
    val projectId: Int? = null,
    val answers: Map<Int, Boolean> = emptyMap(),
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val submitted: SafetyInspection? = null,
    val submittedOffline: Boolean = false
)

data class SafetyChecklistUiState(
    val isLoading: Boolean = true,
    val items: List<SafetyChecklistItem> = emptyList(),
    val projects: List<Project> = emptyList(),
    val form: ChecklistForm = ChecklistForm(),
    val lastToday: SafetyInspection? = null
) {

    val answeredCount: Int
        get() = items.count {
            it.itemId in form.answers
        }

    val result: ChecklistResult
        get() = when {
            items.isEmpty() ->
                ChecklistResult.INCOMPLETE

            items.any {
                form.answers[it.itemId] == false
            } ->
                ChecklistResult.FAIL

            answeredCount < items.size ->
                ChecklistResult.INCOMPLETE

            else ->
                ChecklistResult.PASS
        }

    val failedItems: List<SafetyChecklistItem>
        get() = items.filter {
            form.answers[it.itemId] == false
        }

    val canSubmit: Boolean
        get() =
            items.isNotEmpty() &&
                    answeredCount == items.size &&
                    form.projectId != null &&
                    !form.isSubmitting &&
                    form.submitted == null

    val selectedProject: Project?
        get() = projects.firstOrNull {
            it.projectId == form.projectId
        }
}

/**
 * Safety checklist with recoverable submission failures.
 *
 * A request failure never clears the worker's answers or notes.
 */
class SafetyChecklistViewModel(
    private val authRepository: AuthRepository,
    private val safetyRepository: SafetyRepository,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    private val syncRepository: SyncRepository,
    private val clock: Clock
) : ViewModel() {

    private val form = MutableStateFlow(
        ChecklistForm()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workerData =
        authRepository.session.flatMapLatest { session ->
            if (session == null) {
                return@flatMapLatest flowOf<WorkerData?>(null)
            }

            val userId = session.user.userId

            combine(
                safetyRepository.observeChecklistItems(),
                taskRepository.observeTasksAssignedTo(userId),
                projectRepository.observeProjects(),
                safetyRepository.observeInspectionsBy(userId)
            ) { items, tasks, projects, inspections ->
                val assignedProjectIds = tasks
                    .map { it.projectId }
                    .toSet()

                val accessibleProjects = projects
                    .filter {
                        it.projectId in assignedProjectIds
                    }
                    .ifEmpty {
                        projects.filter {
                            it.status == ProjectStatus.ACTIVE
                        }
                    }

                WorkerData(
                    items = items,
                    projects = accessibleProjects,
                    lastToday = inspections.firstOrNull {
                        it.date == LocalDate.now(clock)
                    }
                )
            }
        }

    val uiState: StateFlow<SafetyChecklistUiState> =
        combine(workerData, form) { data, current ->
            if (data == null) {
                return@combine SafetyChecklistUiState()
            }

            SafetyChecklistUiState(
                isLoading = false,
                items = data.items,
                projects = data.projects,
                form = current.copy(
                    projectId = current.projectId
                        ?: data.projects.firstOrNull()?.projectId
                ),
                lastToday = data.lastToday
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                STOP_TIMEOUT_MS
            ),
            initialValue = SafetyChecklistUiState()
        )

    fun onProjectSelected(projectId: Int) {
        if (isFormLocked()) {
            return
        }

        form.update {
            it.copy(
                projectId = projectId
            )
        }
    }

    fun onAnswer(
        itemId: Int,
        isSafe: Boolean
    ) {
        if (isFormLocked()) {
            return
        }

        if (uiState.value.items.none {
                it.itemId == itemId
            }) {
            return
        }

        form.update {
            it.copy(
                answers = it.answers + (
                        itemId to isSafe
                        )
            )
        }
    }

    fun onNotesChange(text: String) {
        if (isFormLocked()) {
            return
        }

        form.update {
            it.copy(
                notes = text
            )
        }
    }

    fun onSubmit() {
        val state = uiState.value

        /*
         * Read the backing form as well as uiState.
         * uiState may not yet have emitted the previous busy update.
         */
        if (
            form.value.isSubmitting ||
            form.value.submitted != null ||
            state.isLoading ||
            !state.canSubmit
        ) {
            return
        }

        val userId =
            authRepository.session.value?.user?.userId

        if (userId == null) {
            BackendFeedback.reportFailure(
                "Sign in before submitting the safety checklist. " +
                        "Your answers and notes are still available."
            )
            return
        }

        val projectId = state.form.projectId
            ?: return

        /*
         * Snapshot the submitted data before starting the request.
         * Editing is blocked while this request is running.
         */
        val passed =
            state.result == ChecklistResult.PASS

        val comments =
            inspectionComments(state)

        val submittedOffline =
            !syncRepository.status.value.isOnline

        form.update {
            it.copy(
                projectId = projectId,
                isSubmitting = true
            )
        }

        viewModelScope.launch {
            try {
                val inspection =
                    safetyRepository.submitInspection(
                        projectId = projectId,
                        inspectorId = userId,
                        passed = passed,
                        comments = comments
                    )

                /*
                 * Do not show an old account's result if the user
                 * signed out or switched accounts during the request.
                 */
                if (
                    authRepository.session.value
                        ?.user?.userId != userId
                ) {
                    return@launch
                }

                form.update {
                    it.copy(
                        submitted = inspection,
                        submittedOffline = submittedOffline
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                if (
                    authRepository.session.value
                        ?.user?.userId == userId
                ) {
                    BackendFeedback.reportFailure(
                        "The safety checklist submission could not " +
                                "be confirmed. Your answers and notes " +
                                "have been kept. Check your connection " +
                                "and today's checklist history before " +
                                "trying again."
                    )
                }
            } finally {
                form.update {
                    it.copy(
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun onStartAgain() {
        if (form.value.isSubmitting) {
            return
        }

        val selectedProjectId =
            form.value.projectId
                ?: uiState.value.form.projectId

        form.value = ChecklistForm(
            projectId = selectedProjectId
        )
    }

    private fun isFormLocked(): Boolean {
        val current = form.value

        return current.isSubmitting ||
                current.submitted != null
    }

    private fun inspectionComments(
        state: SafetyChecklistUiState
    ): String {
        val failed = state.failedItems
            .joinToString("; ") {
                it.title
            }

        val summary = if (failed.isNotEmpty()) {
            "Failed: $failed"
        } else {
            "All items passed"
        }

        return listOf(
            summary,
            state.form.notes.trim()
        ).filter {
            it.isNotEmpty()
        }.joinToString(". ")
    }

    private data class WorkerData(
        val items: List<SafetyChecklistItem>,
        val projects: List<Project>,
        val lastToday: SafetyInspection?
    )

    companion object {

        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()

                SafetyChecklistViewModel(
                    authRepository =
                        container.authRepository,
                    safetyRepository =
                        container.safetyRepository,
                    projectRepository =
                        container.projectRepository,
                    taskRepository =
                        container.taskRepository,
                    syncRepository =
                        container.syncRepository,
                    clock =
                        container.clock
                )
            }
        }
    }
}