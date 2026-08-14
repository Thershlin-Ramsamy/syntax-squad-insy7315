package com.mcc.construction.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.data.Task
import com.mcc.construction.data.TaskStatus
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.theme.ConstructionOrange
import com.mcc.construction.ui.theme.SuccessGreen

@Composable
fun WorkerDashboardScreen(navController: NavHostController) {
    val worker = MockRepository.currentWorker
    val tasks = MockRepository.tasks
    val isClockedIn by MockRepository.isClockedIn

    val pendingCount = tasks.count { it.status != TaskStatus.COMPLETED }
    val completedCount = tasks.count { it.status == TaskStatus.COMPLETED }
    val todaysTasks = tasks.filter { it.status != TaskStatus.COMPLETED }.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Good day, ${worker.name.substringBefore(" ")}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = worker.site,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(label = "Open Tasks", value = pendingCount.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "Completed", value = completedCount.toString(), modifier = Modifier.weight(1f))
        }

        Button(
            onClick = { MockRepository.toggleClockedIn() },
            colors = if (isClockedIn) {
                ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            } else {
                ButtonDefaults.buttonColors(containerColor = ConstructionOrange)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(56.dp)
        ) {
            Icon(Icons.Filled.AccessTime, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text(
                text = if (isClockedIn) "Clocked In — tap to clock out" else "Clock In",
                style = MaterialTheme.typography.labelLarge,
                color = if (isClockedIn) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondary
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .border(1.dp, ConstructionOrange, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = ConstructionOrange)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Column {
                    Text("Safety reminder", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Complete today's safety checklist before starting work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today's Tasks", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { navController.navigate(Routes.TASKS) }) {
                Text("View All")
            }
        }

        todaysTasks.forEach { task ->
            DashboardTaskCard(
                task = task,
                onClick = { navController.navigate(Routes.taskDetail(task.id)) }
            )
            Spacer(Modifier.padding(top = 4.dp))
        }

        if (todaysTasks.isEmpty()) {
            Text(
                "No open tasks — nice work!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardTaskCard(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${task.projectName} • ${task.dueTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(status = task.status)
        }
    }
}
