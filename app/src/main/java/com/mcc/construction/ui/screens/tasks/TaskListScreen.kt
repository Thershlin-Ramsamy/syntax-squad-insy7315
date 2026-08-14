package com.mcc.construction.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.data.Task
import com.mcc.construction.data.TaskPriority
import com.mcc.construction.data.TaskStatus
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.theme.DangerRed

private enum class TaskFilter(val label: String) {
    ALL("All"), PENDING("Pending"), IN_PROGRESS("In Progress"), COMPLETED("Completed")
}

@Composable
fun TaskListScreen(navController: NavHostController) {
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    val tasks = MockRepository.tasks

    val filteredTasks = when (selectedFilter) {
        TaskFilter.ALL -> tasks
        TaskFilter.PENDING -> tasks.filter { it.status == TaskStatus.PENDING }
        TaskFilter.IN_PROGRESS -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
        TaskFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TaskFilter.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredTasks, key = { it.id }) { task ->
                TaskRow(task = task, onClick = { navController.navigate(Routes.taskDetail(task.id)) })
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.priority == TaskPriority.HIGH) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${task.projectName} • ${task.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    task.dueTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(status = task.status)
        }
    }
}
