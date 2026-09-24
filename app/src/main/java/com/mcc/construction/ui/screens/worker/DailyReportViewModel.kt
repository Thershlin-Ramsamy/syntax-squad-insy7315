package com.mcc.construction.ui.screens.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.Weather
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.DailyReportDraft
import com.mcc.construction.data.repository.DailyReportRepository
import com.mcc.construction.data.repository.ProjectRepository
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

/** Parts of the report the worker still has to fill in before it can be sent. */
enum class ReportField { PROJECT, WEATHER, WORK_COMPLETED }

/** What the worker has entered so far. */
data class DailyReportForm(
    val projectId: Int? = null,
    val weather: Weather? = null,
    val workCompleted: String = "",
    val problems: String = "",
    val photoUris: List<String> = emptyList(),
    val missing: Set<ReportField> = emptySet(),
    val isConfirmVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitted: DailyReport? = null,
    val submittedOffline: Boolean = false
)

data class DailyReportUiState(
    val isLoading: Boolean = true,
    /** The worker's own projects first; every active project if they have none. */
    val projects: List<Project> = emptyList(),
    val form: DailyReportForm = DailyReportForm(),
    /** Titles of the worker's tasks on the chosen project that are already done. */
    val finishedTaskTitles: List<String> = emptyList(),
    /** A report already sent today for the chosen project, if any. */
    val alreadySubmittedToday: DailyReport? = null
) {
    val selectedProject: Project? get() = projects.firstOrNull { it.projectId == form.projectId }
}

/**
 * The end-of-day report: project, weather, what was done, what got in the way,
 * and photos. Choices are chips rather than typing wherever the answer is one
 * of a few, and finished tasks can be pulled in with one tap.
 */
class DailyReportViewModel(
    private val authRepository: AuthRepository,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    private val reportRepository: DailyReportRepository,
    private val syncRepository: SyncRepository,
    private val clock: Clock
) : ViewModel() {

    private val form = MutableStateFlow(DailyReportForm())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workerData = authRepository.session.flatMapLatest { session ->
        if (session == null) return@flatMapLatest flowOf(null)
        val userId = session.user.userId
        combine(
            taskRepository.observeTasksAssignedTo(userId),
            projectRepository.observeProjects(),
            reportRepository.observeReportsBy(userId)
        ) { tasks, projects, reports -> Triple(tasks, projects, reports) }
    }

    val uiState: StateFlow<DailyReportUiState> = combine(workerData, form) { data, current ->
        if (data == null) return@combine DailyReportUiState()
        val (tasks, allProjects, reports) = data
        val mine = tasks.map { it.projectId }.toSet()
        val projects = allProjects.filter { it.projectId in mine }
            .ifEmpty { allProjects.filter { it.status == ProjectStatus.ACTIVE } }
        // Default to the worker's main project so the most common case needs no tap.
        val projectId = current.projectId ?: projects.firstOrNull()?.projectId
        val today = LocalDate.now(clock)
        DailyReportUiState(
            isLoading = false,
            projects = projects,
            form = current.copy(projectId = projectId),
            finishedTaskTitles = tasks
                .filter { it.projectId == projectId && it.status == TaskStatus.COMPLETED }
                .map { it.title },
            alreadySubmittedToday = reports.firstOrNull { it.projectId == projectId && it.date == today }
                ?.takeIf { current.submitted == null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DailyReportUiState())

    fun onProjectSelected(projectId: Int) = edit { it.copy(projectId = projectId, missing = it.missing - ReportField.PROJECT) }

    fun onWeatherSelected(weather: Weather) = edit { it.copy(weather = weather, missing = it.missing - ReportField.WEATHER) }

    fun onWorkCompletedChange(text: String) =
        edit { it.copy(workCompleted = text, missing = it.missing - ReportField.WORK_COMPLETED) }

    fun onProblemsChange(text: String) = edit { it.copy(problems = text) }

    /** Adds the finished tasks to "work completed", one per line, skipping any already there. */
    fun onAddFinishedTasks() {
        val titles = uiState.value.finishedTaskTitles
        edit { current ->
            val missingTitles = titles.filterNot { current.workCompleted.contains(it) }
            if (missingTitles.isEmpty()) return@edit current
            current.copy(
                workCompleted = appendLines(current.workCompleted, missingTitles),
                missing = current.missing - ReportField.WORK_COMPLETED
            )
        }
    }

    /** Adds a common problem from a quick chip; the screen passes the translated phrase. */
    fun onAddProblem(phrase: String) = edit { current ->
        if (current.problems.contains(phrase)) current
        else current.copy(problems = appendLines(current.problems, listOf(phrase)))
    }

    fun onPhotosPicked(uris: List<String>) = edit { current ->
        current.copy(photoUris = (current.photoUris + uris).distinct().take(MAX_PHOTOS))
    }

    fun onRemovePhoto(uri: String) = edit { it.copy(photoUris = it.photoUris - uri) }

    /** Checks the report is complete, then asks for confirmation before sending. */
    fun onSubmitClick() {
        val current = uiState.value.form
        val missing = buildSet {
            if (current.projectId == null) add(ReportField.PROJECT)
            if (current.weather == null) add(ReportField.WEATHER)
            if (current.workCompleted.isBlank()) add(ReportField.WORK_COMPLETED)
        }
        edit { it.copy(missing = missing, isConfirmVisible = missing.isEmpty()) }
    }

    fun onDismissConfirm() = edit { it.copy(isConfirmVisible = false) }

    fun onConfirmSubmit() {
        val userId = authRepository.session.value?.user?.userId ?: return
        val current = uiState.value.form
        val projectId = current.projectId ?: return
        val weather = current.weather ?: return
        edit { it.copy(isConfirmVisible = false, isSubmitting = true) }
        viewModelScope.launch {
            val report = reportRepository.submitReport(
                DailyReportDraft(
                    projectId = projectId,
                    userId = userId,
                    weather = weather.storedValue,
                    workCompleted = current.workCompleted.trim(),
                    problemsEncountered = current.problems.trim(),
                    photoUris = current.photoUris
                )
            )
            val offline = !syncRepository.status.value.isOnline
            edit { it.copy(isSubmitting = false, submitted = report, submittedOffline = offline) }
        }
    }

    /** Clears the form for another report, keeping the chosen project. */
    fun onStartAnother() = edit { DailyReportForm(projectId = it.projectId) }

    private fun edit(change: (DailyReportForm) -> DailyReportForm) {
        form.update { change(it.copy(projectId = it.projectId ?: uiState.value.form.projectId)) }
    }

    private fun appendLines(existing: String, lines: List<String>): String =
        (listOf(existing.trimEnd()).filter { it.isNotEmpty() } + lines).joinToString("\n")

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_PHOTOS = 5

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                DailyReportViewModel(
                    c.authRepository, c.projectRepository, c.taskRepository,
                    c.dailyReportRepository, c.syncRepository, c.clock
                )
            }
        }
    }
}
