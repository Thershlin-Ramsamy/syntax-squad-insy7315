package com.mcc.construction.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.mcc.construction.R
import com.mcc.construction.data.model.UserRole

/** One destination in the navigation bar or rail. */
data class NavItem(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
)

/**
 * Which destinations each role sees, and how many fit the window.
 *
 * Every role has a short **primary** list — the bottom bar on a phone — and a
 * **secondary** list that the rail adds on wider windows, where there is room
 * for the fuller menu. Both are subsets of one navigation graph: the Inventory
 * reached from a manager's rail is the same destination reached from a
 * dashboard shortcut on a phone.
 */
object NavMenus {

    private val home = NavItem(Routes.WORKER_DASHBOARD, R.string.nav_home, Icons.Filled.Home)
    private val tasks = NavItem(Routes.TASKS, R.string.nav_tasks, Icons.AutoMirrored.Filled.Assignment)
    private val messages = NavItem(Routes.MESSAGES, R.string.nav_messages, Icons.AutoMirrored.Filled.Chat)
    private val profile = NavItem(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person)
    private val attendance = NavItem(Routes.ATTENDANCE, R.string.nav_attendance, Icons.Filled.AccessTime)
    private val dailyReport = NavItem(Routes.DAILY_REPORT, R.string.nav_daily_report, Icons.Filled.Description)
    private val safety = NavItem(Routes.SAFETY_CHECKLIST, R.string.nav_safety, Icons.Filled.HealthAndSafety)
    private val documents = NavItem(Routes.DOCUMENTS, R.string.nav_documents, Icons.Filled.FolderOpen)
    private val settings = NavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)

    private val managerDashboard =
        NavItem(Routes.MANAGER_DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard)
    private val projects = NavItem(Routes.PROJECTS, R.string.nav_projects, Icons.Filled.Work)
    private val employees = NavItem(Routes.EMPLOYEES, R.string.nav_employees, Icons.Filled.Groups)
    private val inventory = NavItem(Routes.INVENTORY, R.string.nav_inventory, Icons.Filled.Inventory2)
    private val reports = NavItem(Routes.REPORTS, R.string.nav_reports, Icons.Filled.Assessment)

    private val adminDashboard =
        NavItem(Routes.ADMIN_DASHBOARD, R.string.nav_dashboard, Icons.Filled.AdminPanelSettings)
    private val users = NavItem(Routes.USER_MANAGEMENT, R.string.nav_users, Icons.Filled.ManageAccounts)
    private val auditLog = NavItem(Routes.AUDIT_LOG, R.string.nav_audit_log, Icons.AutoMirrored.Filled.FactCheck)
    private val systemSettings = NavItem(Routes.SYSTEM_SETTINGS, R.string.nav_system, Icons.Filled.Tune)

    /** The bottom bar on a phone, and the first items on the rail. */
    fun primary(role: UserRole): List<NavItem> = when (role) {
        UserRole.WORKER -> listOf(home, tasks, messages, profile)
        UserRole.MANAGER -> listOf(managerDashboard, projects, employees, inventory, reports)
        UserRole.ADMINISTRATOR -> listOf(adminDashboard, users, auditLog, profile)
    }

    /** Added to the rail on wider windows. */
    fun secondary(role: UserRole): List<NavItem> = when (role) {
        UserRole.WORKER -> listOf(attendance, dailyReport, safety, documents)
        UserRole.MANAGER -> listOf(tasks, documents, safety, settings)
        UserRole.ADMINISTRATOR -> listOf(systemSettings, employees, reports, settings)
    }

    /**
     * The items to show for [role]. The phone bar shows only the primary list;
     * the rail adds as many secondary items as the window height allows, so a
     * short landscape window never clips the bottom of the rail.
     */
    fun visibleItems(role: UserRole, compactWidth: Boolean, windowHeightDp: Int): List<NavItem> {
        val primary = primary(role)
        if (compactWidth) return primary
        val fits = ((windowHeightDp - RAIL_RESERVED_DP) / RAIL_ITEM_DP).coerceAtLeast(primary.size)
        return (primary + secondary(role)).take(fits)
    }

    /** The landing destination after signing in as [role]. */
    fun homeRoute(role: UserRole): String = when (role) {
        UserRole.WORKER -> Routes.WORKER_DASHBOARD
        UserRole.MANAGER -> Routes.MANAGER_DASHBOARD
        UserRole.ADMINISTRATOR -> Routes.ADMIN_DASHBOARD
    }

    /**
     * Destinations drawn inside the navigation chrome: every menu item for any
     * role, plus the few shared screens reached from the app bar. Everything
     * else — sign-in, detail pages, forms — takes the full window with its own
     * back button.
     */
    val chromeRoutes: Set<String> =
        UserRole.entries.flatMap { primary(it) + secondary(it) }.map { it.route }.toSet() +
            setOf(Routes.NOTIFICATIONS, Routes.PROFILE, Routes.SETTINGS)

    /** App bar title for a destination inside the chrome. */
    @StringRes
    fun titleRes(route: String?): Int = when (route) {
        Routes.WORKER_DASHBOARD, Routes.MANAGER_DASHBOARD -> R.string.title_dashboard
        Routes.ADMIN_DASHBOARD -> R.string.title_administration
        Routes.TASKS -> R.string.title_tasks
        Routes.ATTENDANCE -> R.string.title_attendance
        Routes.MESSAGES -> R.string.title_messages
        Routes.PROFILE -> R.string.title_profile
        Routes.SETTINGS -> R.string.title_settings
        Routes.NOTIFICATIONS -> R.string.title_notifications
        Routes.DAILY_REPORT -> R.string.title_daily_report
        Routes.SAFETY_CHECKLIST -> R.string.title_safety_checklist
        Routes.DOCUMENTS -> R.string.title_documents
        Routes.PROJECTS -> R.string.title_projects
        Routes.EMPLOYEES -> R.string.title_employees
        Routes.INVENTORY -> R.string.title_inventory
        Routes.REPORTS -> R.string.title_reports
        Routes.USER_MANAGEMENT -> R.string.title_user_management
        Routes.AUDIT_LOG -> R.string.title_audit_log
        Routes.SYSTEM_SETTINGS -> R.string.title_system_settings
        else -> R.string.app_name
    }

    /** Space kept for the status bar and rail padding. */
    private const val RAIL_RESERVED_DP = 48

    /** A rail item (56dp) plus the gap between items, rounded up. */
    private const val RAIL_ITEM_DP = 64
}
