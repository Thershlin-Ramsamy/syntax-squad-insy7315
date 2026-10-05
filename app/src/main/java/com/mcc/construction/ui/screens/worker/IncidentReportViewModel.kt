package com.mcc.construction.ui.screens.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.*
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.launchSubmission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

enum class IncidentField {
    SEVERITY, DESCRIPTION
}

data class IncidentForm(
    val projectId: Int? = null,
    val severity: IncidentSeverity? = null,
    val description: String = "",
    val photoUri: String? = null,
    val missing: Set<IncidentField> = emptySet(),
    val isSubmitting: Boolean = false,
    val submitted: IncidentReport? = null,
    val submittedOffline: Boolean = false
)

data class IncidentReportUiState(
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val linkedInspection: SafetyInspection? = null,
    val form: IncidentForm = IncidentForm()
) {
    val selectedProject: Project?
        get() = projects.firstOrNull { it.projectId == form.projectId }

    val isUrgent: Boolean
        get() = (form.severity?.level ?: 0) >= IncidentSeverity.SERIOUS.level
}

class IncidentReportViewModel(
    private val inspectionId: Int?,
    private val authRepository: AuthRepository,
    private val safetyRepository: SafetyRepository,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val form = MutableStateFlow(IncidentForm())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workerData = authRepository.session.flatMapLatest { session ->
        if (session == null) return@flatMapLatest flowOf(null)

        combine(
            taskRepository.observeTasksAssignedTo(session.user.userId),
            projectRepository.observeProjects(),
            if (inspectionId == null) flowOf(null)
            else safetyRepository.observeInspection(inspectionId)
        ) { tasks, projects, inspection ->
            val mine = tasks.map { it.projectId }.toSet()

            Triple(
                projects.filter { it.projectId in mine }.ifEmpty {
                    projects.filter { it.status == ProjectStatus.ACTIVE }
                },
                projects,
                inspection
            )
        }
    }

    val uiState: StateFlow<IncidentReportUiState> =
        combine(workerData, form) { data, current ->
            if (data == null) return@combine IncidentReportUiState()

            val (mine, all, inspection) = data
            val projectId = inspection?.projectId
                ?: current.projectId
                ?: mine.firstOrNull()?.projectId

            IncidentReportUiState(
                isLoading = false,
                projects = if (inspection != null) {
                    all.filter { it.projectId == inspection.projectId }
                } else mine,
                linkedInspection = inspection,
                form = current.copy(projectId = projectId)
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            IncidentReportUiState()
        )

    fun onProjectSelected(projectId: Int) = edit {
        it.copy(projectId = projectId)
    }

    fun onSeveritySelected(severity: IncidentSeverity) = edit {
        it.copy(
            severity = severity,
            missing = it.missing - IncidentField.SEVERITY
        )
    }

    fun onDescriptionChange(text: String) = edit {
        it.copy(
            description = text,
            missing = it.missing - IncidentField.DESCRIPTION
        )
    }

    fun onAddPhrase(phrase: String) = edit {
        if (it.description.contains(phrase)) it
        else it.copy(
            description = listOf(
                it.description.trimEnd(),
                phrase
            ).filter { text -> text.isNotEmpty() }.joinToString(". "),
            missing = it.missing - IncidentField.DESCRIPTION
        )
    }

    fun onPhotoPicked(uri: String) = edit {
        it.copy(photoUri = uri)
    }

    fun onRemovePhoto() = edit {
        it.copy(photoUri = null)
    }

    fun onSubmit() {
        val state = uiState.value
        val userId = authRepository.session.value?.user?.userId ?: return

        if (
            form.value.isSubmitting ||
            form.value.submitted != null ||
            state.isLoading
        ) return

        val missing = buildSet {
            if (state.form.severity == null) add(IncidentField.SEVERITY)
            if (state.form.description.isBlank()) add(IncidentField.DESCRIPTION)
        }

        val projectId = state.form.projectId
        val severity = state.form.severity

        if (missing.isNotEmpty() || projectId == null || severity == null) {
            form.update { it.copy(missing = missing) }
            return
        }

        form.update {
            it.copy(
                projectId = projectId,
                missing = emptySet()
            )
        }

        launchSubmission(
            isBusy = { form.value.isSubmitting },
            setBusy = { busy ->
                form.update { it.copy(isSubmitting = busy) }
            },
            failureMessage =
                "The incident report could not be confirmed. Your description " +
                        "and photo selection have been kept. Check your connection " +
                        "and report history before trying again."
        ) {
            val incident = safetyRepository.reportIncident(
                IncidentDraft(
                    projectId = projectId,
                    reportedBy = userId,
                    severity = severity,
                    description = state.form.description.trim(),
                    photoUris = listOfNotNull(state.form.photoUri),
                    inspectionId = state.linkedInspection?.inspectionId
                )
            )

            if (authRepository.session.value?.user?.userId == userId) {
                form.update {
                    it.copy(
                        submitted = incident,
                        submittedOffline = false
                    )
                }
            }
        }
    }

    private fun edit(change: (IncidentForm) -> IncidentForm) {
        form.update {
            if (it.isSubmitting || it.submitted != null) it
            else change(it)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(inspectionId: Int?) = viewModelFactory {
            initializer {
                val c = appContainer()
                IncidentReportViewModel(
                    inspectionId,
                    c.authRepository,
                    c.safetyRepository,
                    c.projectRepository,
                    c.taskRepository,
                    c.syncRepository
                )
            }
        }
    }
}