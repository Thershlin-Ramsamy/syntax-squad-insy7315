package com.mcc.construction.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.mcc.construction.data.model.UserRole

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Navigation is role-aware and size-aware, in two different ways.
 *
 * CLAUDE.md specifies that the expanded layout carries "the fuller Manager/Admin
 * menu" — so the difference between compact and expanded is not only the chrome
 * shape (bottom bar vs rail) but the number of destinations on offer. Compact is
 * capped at five, which is the practical limit for a bottom bar; expanded adds
 * the secondary destinations that would otherwise be buried.
 *
 * Both lists are subsets of one navigation graph, so a destination reached from
 * the rail on a tablet is the same destination reached from Profile on a phone.
 */
object NavMenus {

    private val worker = listOf(
        NavItem(Routes.WORKER_DASHBOARD, "Home", Icons.Filled.Home),
        NavItem(Routes.TASKS, "Tasks", Icons.AutoMirrored.Filled.Assignment),
        NavItem(Routes.ATTENDANCE, "Attendance", Icons.Filled.AccessTime),
        NavItem(Routes.MESSAGES, "Messages", Icons.Filled.ChatBubble),
        NavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
    )

    private val workerExtra = listOf(
        NavItem(Routes.DAILY_REPORT, "Daily Report", Icons.Filled.Description),
        NavItem(Routes.SAFETY_CHECKLIST, "Safety", Icons.Filled.HealthAndSafety),
        NavItem(Routes.DOCUMENTS, "Documents", Icons.Filled.FolderOpen),
        NavItem(Routes.NOTIFICATIONS, "Alerts", Icons.Filled.Notifications),
        NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
    )

    private val manager = listOf(
        NavItem(Routes.MANAGER_DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
        NavItem(Routes.PROJECTS, "Projects", Icons.Filled.FolderOpen),
        NavItem(Routes.TASKS, "Tasks", Icons.AutoMirrored.Filled.Assignment),
        NavItem(Routes.EMPLOYEES, "Employees", Icons.Filled.Groups),
        NavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
    )

    private val managerExtra = listOf(
        NavItem(Routes.INVENTORY, "Inventory", Icons.Filled.Inventory2),
        NavItem(Routes.REPORTS, "Reports", Icons.Filled.Assessment),
        NavItem(Routes.DOCUMENTS, "Documents", Icons.Filled.Description),
        NavItem(Routes.SAFETY_CHECKLIST, "Safety", Icons.Filled.HealthAndSafety),
        NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
    )

    private val admin = listOf(
        NavItem(Routes.ADMIN_DASHBOARD, "Dashboard", Icons.Filled.AdminPanelSettings),
        NavItem(Routes.USER_MANAGEMENT, "Users", Icons.Filled.ManageAccounts),
        NavItem(Routes.AUDIT_LOG, "Audit", Icons.AutoMirrored.Filled.FactCheck),
        NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
        NavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
    )

    private val adminExtra = listOf(
        NavItem(Routes.EMPLOYEES, "Employees", Icons.Filled.Groups),
        NavItem(Routes.REPORTS, "Reports", Icons.Filled.Assessment),
        NavItem(Routes.DOCUMENTS, "Documents", Icons.Filled.Description),
        NavItem(Routes.NOTIFICATIONS, "Alerts", Icons.Filled.Notifications)
    )

    /** Up to five destinations — the bottom bar on a phone. */
    fun compact(role: UserRole): List<NavItem> = when (role) {
        UserRole.WORKER -> worker
        UserRole.MANAGER -> manager
        UserRole.ADMINISTRATOR -> admin
    }

    /** The fuller menu shown on the rail at medium width and above. */
    fun expanded(role: UserRole): List<NavItem> = when (role) {
        UserRole.WORKER -> worker + workerExtra
        UserRole.MANAGER -> manager + managerExtra
        UserRole.ADMINISTRATOR -> admin + adminExtra
    }

    /** The landing destination after signing in as [role]. */
    fun homeRoute(role: UserRole): String = when (role) {
        UserRole.WORKER -> Routes.WORKER_DASHBOARD
        UserRole.MANAGER -> Routes.MANAGER_DASHBOARD
        UserRole.ADMINISTRATOR -> Routes.ADMIN_DASHBOARD
    }

    /**
     * Every destination that renders inside the navigation chrome. Union across
     * roles so a route stays chrome-wrapped no matter who reaches it.
     */
    val chromeRoutes: Set<String> =
        (UserRole.entries.flatMap { expanded(it) }.map { it.route }).toSet()
}
