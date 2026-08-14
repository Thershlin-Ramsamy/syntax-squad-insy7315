package com.mcc.construction.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.data.ProjectStatus
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.theme.Spacing

private const val ALL = "All"

@Composable
fun ProjectListScreen(navController: NavHostController) {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    val projects = MockRepository.projects

    val options = remember { listOf(ALL) + ProjectStatus.entries.map { it.label } }
    val filtered = if (filter == ALL) projects else projects.filter { it.status.label == filter }

    Column(Modifier.fillMaxSize()) {
        SegmentedFilter(
            options = options,
            selected = filter,
            onSelect = { filter = it },
            modifier = Modifier.padding(
                horizontal = Spacing.screenH,
                vertical = Spacing.md
            )
        )

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.FolderOpen,
                title = "No $filter projects",
                message = "Nothing matches this filter. Try another status to see " +
                    "the rest of the portfolio."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.screenH,
                    end = Spacing.screenH,
                    top = Spacing.xs,
                    bottom = Spacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { navController.navigate(Routes.projectDetail(project.id)) }
                    )
                }
            }
        }
    }
}
