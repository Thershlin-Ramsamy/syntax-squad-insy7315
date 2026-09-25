package com.mcc.construction.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.ListDetailLayout
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.screens.dashboard.previewManagerDashboard
import com.mcc.construction.ui.theme.Spacing

/**
 * Projects list wired to its ViewModel. On a phone, tapping a project calls
 * [onOpenProject]; on a wide window the project opens in [detailPane] beside
 * the list.
 */
@Composable
fun ProjectListRoute(
    onOpenProject: (Int) -> Unit,
    detailPane: (@Composable (projectId: Int) -> Unit)? = null,
    viewModel: ProjectListViewModel = viewModel(factory = ProjectListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val twoPane = LocalWindowLayout.current.isExpanded && detailPane != null
    ProjectListScreen(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onProjectClick = { id -> if (twoPane) viewModel.onSelectProject(id) else onOpenProject(id) },
        detailPane = if (twoPane) detailPane else null
    )
}

/** Every project with its status, progress, location and deadline, filtered by status. */
@Composable
fun ProjectListScreen(
    state: ProjectListUiState,
    onFilterChange: (ProjectStatus?) -> Unit,
    onProjectClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    detailPane: (@Composable (projectId: Int) -> Unit)? = null
) {
    if (state.projects == null) {
        LoadingState(modifier)
        return
    }
    if (detailPane == null) {
        ScreenScaffold(modifier = modifier, scrollable = false) {
            ProjectColumn(state, onFilterChange, onProjectClick, highlightSelection = false)
        }
        return
    }
    ListDetailLayout(
        selected = state.paneProjectId,
        emptyIcon = Icons.Filled.Work,
        emptyTitle = stringResource(R.string.projects_pick_title),
        emptyMessage = stringResource(R.string.projects_pick_message),
        modifier = modifier,
        list = { ProjectColumn(state, onFilterChange, onProjectClick, highlightSelection = true) },
        detail = detailPane
    )
}

@Composable
private fun ColumnScope.ProjectColumn(
    state: ProjectListUiState,
    onFilterChange: (ProjectStatus?) -> Unit,
    onProjectClick: (Int) -> Unit,
    highlightSelection: Boolean
) {
    val options: List<ProjectStatus?> = listOf(null) + ProjectListViewModel.STATUS_ORDER
    val selected = if (highlightSelection) state.paneProjectId else null
    // Filters scroll away with the list, so a short window still shows projects.
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        item(key = "filters") {
            ChoiceChipGroup(
                options = options,
                selected = state.filter,
                onSelect = onFilterChange,
                label = { status ->
                    val name = if (status == null) stringResource(R.string.filter_all) else stringResource(status.labelRes)
                    stringResource(R.string.filter_with_count, name, state.countFor(status))
                }
            )
        }
        if (state.visible.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.Work,
                    title = stringResource(R.string.projects_empty_title),
                    message = stringResource(R.string.projects_empty_message)
                )
            }
        }
        items(state.visible, key = { it.project.projectId }) { summary ->
            ProjectCard(
                summary = summary,
                today = state.today,
                onClick = { onProjectClick(summary.project.projectId) },
                selected = summary.project.projectId == selected
            )
        }
    }
}

private fun previewState(): ProjectListUiState {
    val dashboard = previewManagerDashboard()
    return ProjectListUiState(projects = dashboard.projects, today = dashboard.today)
}

@DevicePreviews
@Composable
private fun ProjectListPreview() {
    PreviewSurface { ProjectListScreen(previewState(), onFilterChange = {}, onProjectClick = {}) }
}

@DevicePreviews
@Composable
private fun ProjectListTwoPanePreview() {
    PreviewSurface {
        ProjectListScreen(previewState(), onFilterChange = {}, onProjectClick = {}, detailPane = { Text("Project $it") })
    }
}
