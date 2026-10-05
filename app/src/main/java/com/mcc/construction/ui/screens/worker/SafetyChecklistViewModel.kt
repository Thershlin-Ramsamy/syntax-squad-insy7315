package com.mcc.construction.ui.screens.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.*
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.runBackendMutation
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ChecklistResult {
    INCOMPLETE, PASS, FAIL
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
            form.answers.values.any { !it } -> ChecklistResult.FAIL
            answeredCount < items.size -> ChecklistResult.INCOMPLETE
            else -> ChecklistResult.PASS
        }

    val failedItems: List<SafetyChecklistItem>
        get() = items.filter {
            form.answers[it.itemId] == false
        }

    val canSubmit: Boolean
        get() = items.isNotEmpty() &&
                answeredCount == items.size &&
                form.projectId != null

    val selectedProject: Project?
        get() = projects.firstOrNull {
            it.projectId == form.projectId
        }
}

class SafetyChecklistViewModel(
    private val authRepository: AuthRepository,
    private val safetyRepository: SafetyRepository,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    private val syncRepository: SyncRepository,
    private val clock: Clock
) : ViewModel() {

    private val form = MutableStateFlow(ChecklistForm())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workerData = authRepository.session.flatMapLatest { session ->
        if (session == null) return@flatMapLatest flowOf(null)

        val userId = session.user.userId

        combine(
            safetyRepository.observeChecklistItems(),
            taskRepository.observeTasksAssignedTo(userId),
            projectRepository.observeProjects(),
            safetyRepository.observeInspectionsBy(userId)
        ) { items, tasks, projects, inspections ->
            val mine = tasks.map { it.projectId }.toSet()

            WorkerData(
                items = items,
                projects = projects.filter {
                    it.projectId in mine
                }.ifEmpty {
                    projects.filter {
                        it.status == ProjectStatus.ACTIVE
                    }
                },
                lastToday = inspections.firstOrNull {
                    it.date == LocalDate.now(clock)
                }
            )
        }
    }

    val uiState: StateFlow<SafetyChecklistUiState> =
        combine(workerData, form) { data, current ->
            if (data == null) return@combine SafetyChecklistUiState()

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
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            SafetyChecklistUiState()
        )

    fun onProjectSelected(projectId: Int) = form.update {
        it.copy(projectId = projectId)
    }

    fun onAnswer(itemId: Int, isSafe: Boolean) = form.update {
        if (it.isSubmitting) {
            it
        } else {
            it.copy(answers = it.answers + (itemId to isSafe))
        }
    }

    fun onNotesChange(text: String) = form.update {
        it.copy(notes = text)
    }

    fun onSubmit() {
        val state = uiState.value
        val userId = authRepository.session.value?.user?.userId ?: return
        val projectId = state.form.projectId ?: return

        if (!state.canSubmit || state.form.isSubmitting) return

        form.update {
            it.copy(
                isSubmitting = true,
                projectId = projectId
            )
        }

        viewModelScope.launch {
            runBackendMutation(
                onFinally = {
                    form.update { it.copy(isSubmitting = false) }
                }
            ) {
                val inspection = safetyRepository.submitInspection(
                    projectId = projectId,
                    inspectorId = userId,
                    passed = state.result == ChecklistResult.PASS,
                    comments = inspectionComments(state)
                )

                val offline = !syncRepository.status.value.isOnline

                form.update {
                    it.copy(
                        isSubmitting = false,
                        submitted = inspection,
                        submittedOffline = offline
                    )
                }
            }
        }
    }

    fun onStartAgain() = form.update {
        ChecklistForm(projectId = it.projectId)
    }

    private fun inspectionComments(
        state: SafetyChecklistUiState
    ): String {
        val failed = state.failedItems.joinToString("; ") {
            it.title
        }

        return listOf(
            if (failed.isNotEmpty()) {
                "Failed: $failed"
            } else {
                "All items passed"
            },
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
                val c = appContainer()
                SafetyChecklistViewModel(
                    c.authRepository,
                    c.safetyRepository,
                    c.projectRepository,
                    c.taskRepository,
                    c.syncRepository,
                    c.clock
                )
            }
        }
    }
}