package com.mcc.construction.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.ui.common.appContainer
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ProjectListUiState(
    /** Null until the first load, so the screen can tell loading from empty. */
    val projects: List<ProjectSummary>? = null,
    /** Null shows every status. */
    val filter: ProjectStatus? = null,
    val today: LocalDate = LocalDate.MIN,
    val selectedProjectId: Int? = null
) {
    val visible: List<ProjectSummary>
        get() = projects.orEmpty().filter { filter == null || it.project.status == filter }

    fun countFor(status: ProjectStatus?): Int =
        projects.orEmpty().count { status == null || it.project.status == status }

    /** What the side pane shows: the chosen project, or the first in the list. */
    val paneProjectId: Int?
        get() = selectedProjectId?.takeIf { id -> visible.any { it.project.projectId == id } }
            ?: visible.firstOrNull()?.project?.projectId
}

/** Every project, filterable by status. */
class ProjectListViewModel(
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    private val clock: Clock
) : ViewModel() {

    private val filter = MutableStateFlow<ProjectStatus?>(null)
    private val selected = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<ProjectListUiState> = combine(
        projectRepository.observeProjects(),
        taskRepository.observeTasks(),
        filter,
        selected
    ) { projects, tasks, status, selectedId ->
        ProjectListUiState(
            projects = projects
                .sortedWith(compareBy({ STATUS_ORDER.indexOf(it.status) }, { it.endDate }))
                .map { ProjectSummary.of(it, tasks) },
            filter = status,
            today = LocalDate.now(clock),
            selectedProjectId = selectedId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProjectListUiState())

    fun onFilterChange(status: ProjectStatus?) {
        filter.update { status }
    }

    fun onSelectProject(projectId: Int) {
        selected.update { projectId }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Live work first, finished work last. */
        val STATUS_ORDER = listOf(ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD, ProjectStatus.PLANNING, ProjectStatus.COMPLETED)

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ProjectListViewModel(c.projectRepository, c.taskRepository, c.clock)
            }
        }
    }
}
