package com.mcc.construction.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.data.TaskPriority
import com.mcc.construction.data.TaskStatus
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.components.TrailingStack
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.tone

private const val ALL = "All"

@Composable
fun TaskListScreen(navController: NavHostController) {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    val tasks = MockRepository.tasks

    val options = remember { listOf(ALL) + TaskStatus.entries.map { it.label } }
    val filtered = if (filter == ALL) tasks else tasks.filter { it.status.label == filter }

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
                icon = Icons.AutoMirrored.Filled.Assignment,
                title = "No $filter tasks".lowercase().replaceFirstChar { it.uppercase() },
                message = "Nothing here right now. Try a different filter to see " +
                    "the rest of your work."
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
                items(filtered, key = { it.id }) { task ->
                    ListRow(
                        title = task.title,
                        subtitle = "${task.projectName} · ${task.location}",
                        meta = task.dueTime,
                        onClick = { navController.navigate(Routes.taskDetail(task.id)) },
                        trailing = {
                            TrailingStack {
                                StatusPill(task.status.label, task.status.tone)
                                if (task.priority == TaskPriority.HIGH) {
                                    Spacer(Modifier.height(Spacing.sm))
                                    Text(
                                        text = "High priority",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
