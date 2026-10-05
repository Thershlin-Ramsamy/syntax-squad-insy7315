package com.mcc.construction.ui.screens.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.*
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.launchSubmission
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

enum class ReportField {
    PROJECT, WEATHER, WORK_COMPLETED
}

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
    val projects: List<Project> = emptyList(),
    val form: DailyReportForm = DailyReportForm(),
    val finishedTaskTitles: List<String> = emptyList(),
    val alreadySubmittedToday: DailyReport? = null
) {
    val selectedProject: Project?
        get() = projects.firstOrNull { it.projectId == form.projectId }
}

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
        ) { tasks, projects, reports ->
            Triple(tasks, projects, reports)
        }
    }

    val uiState: StateFlow<DailyReportUiState> =
        combine(workerData, form) { data, current ->
            if (data == null) return@combine DailyReportUiState()

            val (tasks, allProjects, reports) = data
            val mine = tasks.map { it.projectId }.toSet()
            val projects = allProjects.filter { it.projectId in mine }
                .ifEmpty {
                    allProjects.filter { it.status == ProjectStatus.ACTIVE }
                }
            val projectId = current.projectId
                ?: projects.firstOrNull()?.projectId

            DailyReportUiState(
                isLoading = false,
                projects = projects,
                form = current.copy(projectId = projectId),
                finishedTaskTitles = tasks.filter {
                    it.projectId == projectId &&
                            it.status == TaskStatus.COMPLETED
                }.map { it.title },
                alreadySubmittedToday = reports.firstOrNull {
                    it.projectId == projectId &&
                            it.date == LocalDate.now(clock)
                }?.takeIf { current.submitted == null }
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            DailyReportUiState()
        )

    fun onProjectSelected(projectId: Int) = edit {
        it.copy(
            projectId = projectId,
            missing = it.missing - ReportField.PROJECT
        )
    }

    fun onWeatherSelected(weather: Weather) = edit {
        it.copy(
            weather = weather,
            missing = it.missing - ReportField.WEATHER
        )
    }

    fun onWorkCompletedChange(text: String) = edit {
        it.copy(
            workCompleted = text,
            missing = it.missing - ReportField.WORK_COMPLETED
        )
    }

    fun onProblemsChange(text: String) = edit {
        it.copy(problems = text)
    }

    fun onAddFinishedTasks() {
        val titles = uiState.value.finishedTaskTitles
        edit { current ->
            val missing = titles.filterNot {
                current.workCompleted.contains(it)
            }
            current.copy(
                workCompleted = appendLines(
                    current.workCompleted,
                    missing
                ),
                missing = current.missing - ReportField.WORK_COMPLETED
            )
        }
    }

    fun onAddProblem(phrase: String) = edit {
        if (it.problems.contains(phrase)) it
        else it.copy(
            problems = appendLines(it.problems, listOf(phrase))
        )
    }

    fun onPhotosPicked(uris: List<String>) = edit {
        it.copy(
            photoUris = (it.photoUris + uris).distinct().take(MAX_PHOTOS)
        )
    }

    fun onRemovePhoto(uri: String) = edit {
        it.copy(photoUris = it.photoUris - uri)
    }

    fun onSubmitClick() {
        val current = uiState.value.form
        val missing = missingFields(current)

        edit {
            it.copy(
                missing = missing,
                isConfirmVisible = missing.isEmpty()
            )
        }
    }

    fun onDismissConfirm() = edit {
        it.copy(isConfirmVisible = false)
    }

    fun onConfirmSubmit() {
        val userId = authRepository.session.value?.user?.userId ?: return
        val current = uiState.value.form

        if (form.value.isSubmitting || form.value.submitted != null) return

        val missing = missingFields(current)
        if (missing.isNotEmpty()) {
            edit {
                it.copy(
                    missing = missing,
                    isConfirmVisible = false
                )
            }
            return
        }

        val projectId = current.projectId ?: return
        val weather = current.weather ?: return

        form.update {
            it.copy(
                projectId = projectId,
                isConfirmVisible = false
            )
        }

        launchSubmission(
            isBusy = { form.value.isSubmitting },
            setBusy = { busy ->
                form.update { it.copy(isSubmitting = busy) }
            },
            failureMessage =
                "The daily report could not be confirmed. Your text and " +
                        "photo selections have been kept. Check your connection " +
                        "and report history before trying again."
        ) {
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

            if (authRepository.session.value?.user?.userId == userId) {
                form.update {
                    it.copy(
                        submitted = report,
                        submittedOffline = false
                    )
                }
            }
        }
    }

    fun onStartAnother() {
        if (form.value.isSubmitting) return

        form.value = DailyReportForm(
            projectId = form.value.projectId
                ?: uiState.value.form.projectId
        )
    }

    private fun missingFields(
        current: DailyReportForm
    ): Set<ReportField> = buildSet {
        if (current.projectId == null) add(ReportField.PROJECT)
        if (current.weather == null) add(ReportField.WEATHER)
        if (current.workCompleted.isBlank()) add(ReportField.WORK_COMPLETED)
    }

    private fun edit(change: (DailyReportForm) -> DailyReportForm) {
        form.update {
            if (it.isSubmitting || it.submitted != null) it
            else change(
                it.copy(
                    projectId = it.projectId
                        ?: uiState.value.form.projectId
                )
            )
        }
    }

    private fun appendLines(
        existing: String,
        lines: List<String>
    ): String =
        (listOf(existing.trimEnd()).filter { it.isNotEmpty() } + lines)
            .joinToString("\n")

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_PHOTOS = 5

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                DailyReportViewModel(
                    c.authRepository,
                    c.projectRepository,
                    c.taskRepository,
                    c.dailyReportRepository,
                    c.syncRepository,
                    c.clock
                )
            }
        }
    }
}