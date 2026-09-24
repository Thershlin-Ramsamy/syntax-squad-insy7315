package com.mcc.construction.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.EmployeeStatus
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.data.legacy.ProjectStatus
import com.mcc.construction.data.legacy.StockLevel
import com.mcc.construction.data.legacy.TaskStatus
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.MetricTile
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.screens.projects.ProjectCard
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

/**
 * Manager home.
 *
 * Where the worker dashboard answers "what do I do next", this one answers
 * "what needs me". Exceptions come first — blocked tasks and stock-outs are
 * surfaced as banners above the fold — then the portfolio, then the crew.
 */
@Composable
fun ManagerDashboardScreen(navController: NavHostController) {
    val user = MockRepository.currentUser
    val tasks = MockRepository.tasks
    val projects = MockRepository.projects
    val employees = MockRepository.employees
    val materials = MockRepository.materials

    val activeProjects = projects.filter { it.status == ProjectStatus.ACTIVE }
    val openTasks = tasks.count { it.status != TaskStatus.COMPLETED }
    val blocked = tasks.filter { it.status == TaskStatus.BLOCKED }
    val onSite = employees.count { it.status == EmployeeStatus.ON_SITE }
    val needsReorder = materials.filter { it.stockLevel != StockLevel.IN_STOCK }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            Text(
                text = "Good morning, ${user.name.substringBefore(" ")}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${activeProjects.size} active projects · $onSite crew on site",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.xl))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
                MetricTile(
                    value = activeProjects.size.toString(),
                    label = "Active projects",
                    icon = Icons.Filled.FolderOpen,
                    tone = Tone.Info,
                    onClick = { navController.navigate(Routes.PROJECTS) },
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = openTasks.toString(),
                    label = "Open tasks",
                    icon = Icons.Filled.PendingActions,
                    tone = Tone.Warning,
                    onClick = { navController.navigate(Routes.TASKS) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(Spacing.itemGap))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
                MetricTile(
                    value = onSite.toString(),
                    label = "Crew on site",
                    icon = Icons.Filled.Groups,
                    tone = Tone.Success,
                    onClick = { navController.navigate(Routes.EMPLOYEES) },
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = needsReorder.size.toString(),
                    label = "Stock alerts",
                    icon = Icons.Filled.Inventory2,
                    tone = if (needsReorder.isEmpty()) Tone.Neutral else Tone.Danger,
                    onClick = { navController.navigate(Routes.INVENTORY) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (blocked.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                Banner(
                    title = "${blocked.size} blocked task${if (blocked.size == 1) "" else "s"}",
                    message = blocked.first().title + " — " + blocked.first().dueTime,
                    icon = Icons.Filled.WarningAmber,
                    tone = Tone.Danger
                )
            }

            if (needsReorder.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.itemGap))
                Banner(
                    title = "Stock needs attention",
                    message = needsReorder.joinToString(", ") { it.name } +
                        " at or below reorder level.",
                    icon = Icons.Filled.Inventory2,
                    tone = Tone.Warning
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(
                title = "Projects",
                actionLabel = "View all",
                onAction = { navController.navigate(Routes.PROJECTS) }
            )
            Spacer(Modifier.height(Spacing.blockGap))

            activeProjects.forEachIndexed { index, project ->
                if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                ProjectCard(
                    project = project,
                    onClick = { navController.navigate(Routes.projectDetail(project.id)) }
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Quick actions")
            Spacer(Modifier.height(Spacing.blockGap))

            ListRow(
                title = "Employees",
                subtitle = "$onSite of ${employees.size} on site today",
                onClick = { navController.navigate(Routes.EMPLOYEES) },
                leading = { LeadingIcon(Icons.Filled.Groups, Tone.Info) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Inventory",
                subtitle = "${materials.size} materials tracked",
                onClick = { navController.navigate(Routes.INVENTORY) },
                leading = { LeadingIcon(Icons.Filled.Inventory2, Tone.Warning) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Reports",
                subtitle = "${MockRepository.dailyReports.size} daily reports submitted",
                onClick = { navController.navigate(Routes.REPORTS) },
                leading = { LeadingIcon(Icons.Filled.Assessment, Tone.Success) }
            )
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}
