package com.mcc.construction.ui.screens.employees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.DetailScaffold
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeeProfileScreen(navController: NavHostController, employeeId: String) {
    val employee = MockRepository.getEmployee(employeeId)

    DetailScaffold(
        title = "Employee",
        onBack = { navController.popBackStack() }
    ) {
        if (employee == null) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "Employee not found",
                message = "This person may have left the company or moved site.",
                actionLabel = "Go back",
                onAction = { navController.popBackStack() }
            )
            return@DetailScaffold
        }

        val assigned = MockRepository.tasks.filter { it.assignedTo == employee.name }

        ScreenPadding {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(employee.name, size = 64.dp)
                Spacer(Modifier.width(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = employee.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = employee.jobTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    StatusPill(employee.status.label, employee.status.tone, showDot = true)
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            SecondaryButton(
                text = "Message ${employee.name.substringBefore(" ")}",
                icon = Icons.Filled.Email,
                onClick = { navController.navigate(Routes.MESSAGES) }
            )

            Spacer(Modifier.height(Spacing.sectionGap))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(Icons.Filled.Phone, "Phone", employee.phone)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Email, "Email", employee.email)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.FolderOpen, "Project", employee.project)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.CalendarToday, "Started", employee.startDate)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Badge, "Employee ID", employee.id)
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "This week")
            Spacer(Modifier.height(Spacing.blockGap))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                KeyValueRow("Hours logged", "${employee.hoursThisWeek}h")
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Open tasks", assigned.size.toString())
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Certifications")
            Spacer(Modifier.height(Spacing.blockGap))
            if (employee.certifications.isEmpty()) {
                Text(
                    text = "No certifications on file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    employee.certifications.forEach { certification ->
                        StatusPill(certification, Tone.Info)
                    }
                }
            }

            if (assigned.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sectionGap))
                SectionHeader(title = "Assigned tasks")
                Spacer(Modifier.height(Spacing.blockGap))
                assigned.forEachIndexed { index, task ->
                    if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                    ListRow(
                        title = task.title,
                        subtitle = task.location,
                        meta = task.dueTime,
                        onClick = { navController.navigate(Routes.taskDetail(task.id)) },
                        trailing = { StatusPill(task.status.label, task.status.tone) }
                    )
                }
            }
        }
    }
}
