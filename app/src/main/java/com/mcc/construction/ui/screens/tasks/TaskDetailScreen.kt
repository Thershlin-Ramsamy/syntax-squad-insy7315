package com.mcc.construction.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.data.TaskPriority
import com.mcc.construction.data.TaskStatus
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.theme.ConstructionOrange
import com.mcc.construction.ui.theme.DangerRed
import com.mcc.construction.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(navController: NavHostController, taskId: String) {
    val task = MockRepository.getTask(taskId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (task == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) {
                Text("This task could not be found.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                StatusChip(status = task.status)
            }

            if (task.priority == TaskPriority.HIGH) {
                Text(
                    text = "High priority",
                    style = MaterialTheme.typography.labelLarge,
                    color = DangerRed,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            DetailRow(icon = Icons.Filled.Business, text = "${task.projectName} — ${task.location}")
            DetailRow(icon = Icons.Filled.Schedule, text = task.dueTime)
            DetailRow(icon = Icons.Filled.Person, text = task.assignedBy)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            task.safetyNote?.let { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .border(1.dp, ConstructionOrange, RoundedCornerShape(14.dp))
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = ConstructionOrange)
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        Column {
                            Text("Safety note", style = MaterialTheme.typography.titleMedium)
                            Text(note, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (task.status != TaskStatus.COMPLETED) {
                Button(
                    onClick = {
                        MockRepository.markComplete(task.id)
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Mark as Complete", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(horizontal = 6.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
