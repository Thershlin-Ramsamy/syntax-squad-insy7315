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
import kotlinx.coroutines.launch

/** Where the checklist stands: still being answered, all clear, or something failed. */
enum class ChecklistResult { INCOMPLETE, PASS, FAIL }

data class ChecklistForm(
    val projectId: Int? = null,
    /** Item id to answer: true is "yes, safe", false is "no, a problem". */
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
    /** The worker's last checklist today, if they already did one. */
    val lastToday: SafetyInspection? = null
) {
    val answeredCount: Int get() = items.count { it.itemId in form.answers }

    val result: ChecklistResult
        get() = when {
            form.answers.values.any { !it } -> ChecklistResult.FAIL
            answeredCount < items.size -> ChecklistResult.INCOMPLETE
            else -> ChecklistResult.PASS
        }

    val failedItems: List<SafetyChecklistItem> get() = items.filter { form.answers[it.itemId] == false }

    val canSubmit: Boolean get() = items.isNotEmpty() && answeredCount == items.size && form.projectId != null

    val selectedProject: Project? get() = projects.firstOrNull { it.projectId == form.projectId }
}

/**
 * The daily site safety checklist. Every item gets a yes or no; the overall
 * result is worked out as the worker goes, and a failed checklist leads
 * straight to an incident report linked to it.
 */
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
                projects = projects.filter { it.projectId in mine }
                    .ifEmpty { projects.filter { it.status == ProjectStatus.ACTIVE } },
                lastToday = inspections.firstOrNull { it.date == LocalDate.now(clock) }
            )
        }
    }

    val uiState: StateFlow<SafetyChecklistUiState> = combine(workerData, form) { data, current ->
        if (data == null) return@combine SafetyChecklistUiState()
        SafetyChecklistUiState(
            isLoading = false,
            items = data.items,
            projects = data.projects,
            form = current.copy(projectId = current.projectId ?: data.projects.firstOrNull()?.projectId),
            lastToday = data.lastToday
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SafetyChecklistUiState())

    fun onProjectSelected(projectId: Int) = form.update { it.copy(projectId = projectId) }

    fun onAnswer(itemId: Int, isSafe: Boolean) = form.update { it.copy(answers = it.answers + (itemId to isSafe)) }

    fun onNotesChange(text: String) = form.update { it.copy(notes = text) }

    fun onSubmit() {
        val state = uiState.value
        val userId = authRepository.session.value?.user?.userId ?: return
        val projectId = state.form.projectId ?: return
        if (!state.canSubmit || state.form.isSubmitting) return
        form.update { it.copy(isSubmitting = true, projectId = projectId) }
        viewModelScope.launch {
            val inspection = safetyRepository.submitInspection(
                projectId = projectId,
                inspectorId = userId,
                passed = state.result == ChecklistResult.PASS,
                comments = inspectionComments(state)
            )
            val offline = !syncRepository.status.value.isOnline
            form.update { it.copy(isSubmitting = false, submitted = inspection, submittedOffline = offline) }
        }
    }

    /** Clears the answers for a fresh checklist, keeping the chosen project. */
    fun onStartAgain() = form.update { ChecklistForm(projectId = it.projectId) }

    /**
     * What goes in `SAFETY_INSPECTIONS.Comments`: the items that failed, then
     * anything the worker added. Item titles are data from the checklist
     * template, so they are stored as they are.
     */
    private fun inspectionComments(state: SafetyChecklistUiState): String {
        val failed = state.failedItems.joinToString("; ") { it.title }
        return listOf(
            if (failed.isNotEmpty()) "Failed: $failed" else "All items passed",
            state.form.notes.trim()
        ).filter { it.isNotEmpty() }.joinToString(". ")
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
                    c.authRepository, c.safetyRepository, c.projectRepository,
                    c.taskRepository, c.syncRepository, c.clock
                )
            }
        }
    }
}
