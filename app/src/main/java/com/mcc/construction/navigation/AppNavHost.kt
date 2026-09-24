package com.mcc.construction.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mcc.construction.R
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.components.PlaceholderScreen
import com.mcc.construction.ui.screens.admin.AdminDashboardScreen
import com.mcc.construction.ui.screens.admin.AuditLogScreen
import com.mcc.construction.ui.screens.admin.UserManagementScreen
import com.mcc.construction.ui.screens.dashboard.ManagerDashboardScreen
import com.mcc.construction.ui.screens.dashboard.WorkerDashboardRoute
import com.mcc.construction.ui.screens.dashboard.WorkerDestination
import com.mcc.construction.ui.screens.documents.DocumentsScreen
import com.mcc.construction.ui.screens.employees.EmployeeListScreen
import com.mcc.construction.ui.screens.employees.EmployeeProfileScreen
import com.mcc.construction.ui.screens.inventory.InventoryScreen
import com.mcc.construction.ui.screens.inventory.MaterialDetailScreen
import com.mcc.construction.ui.screens.login.ForgotPasswordRoute
import com.mcc.construction.ui.screens.login.LoginRoute
import com.mcc.construction.ui.screens.messages.ChatScreen
import com.mcc.construction.ui.screens.messages.MessagesScreen
import com.mcc.construction.ui.screens.notifications.NotificationsRoute
import com.mcc.construction.ui.screens.profile.ProfileRoute
import com.mcc.construction.ui.screens.profile.ProfileShortcut
import com.mcc.construction.ui.screens.projects.ProjectDetailScreen
import com.mcc.construction.ui.screens.projects.ProjectListScreen
import com.mcc.construction.ui.screens.reports.ReportsScreen
import com.mcc.construction.ui.screens.settings.SettingsRoute
import com.mcc.construction.ui.screens.splash.SplashDestination
import com.mcc.construction.ui.screens.splash.SplashRoute
import com.mcc.construction.ui.screens.sync.SyncStatusRoute
import com.mcc.construction.ui.screens.tasks.TaskDetailRoute
import com.mcc.construction.ui.screens.tasks.TaskListRoute
import com.mcc.construction.ui.screens.worker.AttendanceRoute
import com.mcc.construction.ui.screens.worker.DailyReportRoute
import com.mcc.construction.ui.screens.worker.IncidentReportRoute
import com.mcc.construction.ui.screens.worker.SafetyChecklistRoute

/**
 * The navigation graph: one entry per screen.
 *
 * Screens built on the repository layer are added as their `…Route`
 * composable, which creates the screen's ViewModel and turns its callbacks into
 * navigation. Screens still on the legacy MockRepository take the
 * NavController directly until they are migrated. A screen that has a route
 * but no content yet uses [PlaceholderScreen].
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    role: UserRole?,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        // --- Entry ---------------------------------------------------------
        composable(Routes.SPLASH) {
            SplashRoute(
                onFinished = { destination ->
                    val next = when (destination) {
                        SplashDestination.SignIn -> Routes.LOGIN
                        is SplashDestination.Home -> NavMenus.homeRoute(destination.role)
                    }
                    navController.navigate(next) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginRoute(
                onSignedIn = { role ->
                    navController.navigate(NavMenus.homeRoute(role)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordRoute(onBack = { navController.popBackStack() })
        }

        // --- Shared --------------------------------------------------------
        composable(Routes.NOTIFICATIONS) {
            NotificationsRoute(
                onOpen = { type -> navController.navigate(notificationRoute(type, role)) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileRoute(
                onShortcut = { shortcut -> navController.navigate(shortcut.route) },
                onSignedOut = { navController.navigateToSignIn() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onOpenSyncStatus = { navController.navigate(Routes.SYNC_STATUS) })
        }
        composable(Routes.SYNC_STATUS) {
            SyncStatusRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.DOCUMENTS) { DocumentsScreen(navController) }

        // --- Worker --------------------------------------------------------
        composable(Routes.WORKER_DASHBOARD) {
            WorkerDashboardRoute(
                onNavigate = { destination -> navController.navigate(destination.route) },
                onOpenTask = { taskId -> navController.navigate(Routes.taskDetail(taskId.toString())) }
            )
        }
        composable(Routes.TASKS) {
            TaskListRoute(
                onOpenTask = { taskId -> navController.navigate(Routes.taskDetail(taskId.toString())) },
                detailPane = { taskId ->
                    TaskDetailRoute(taskId = taskId, onBack = {}, embedded = true)
                }
            )
        }
        composable(Routes.ATTENDANCE) { AttendanceRoute() }
        composable(Routes.DAILY_REPORT) { DailyReportRoute(onDone = { navController.popBackStack() }) }
        composable(Routes.SAFETY_CHECKLIST) {
            SafetyChecklistRoute(
                onReportIncident = { inspectionId -> navController.navigate(Routes.incidentReport(inspectionId)) },
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.INCIDENT_REPORT_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_INSPECTION_ID) {
                    type = NavType.IntType
                    defaultValue = NO_INSPECTION
                }
            )
        ) { entry ->
            IncidentReportRoute(
                inspectionId = entry.arguments?.getInt(Routes.ARG_INSPECTION_ID)?.takeIf { it != NO_INSPECTION },
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.MESSAGES) { MessagesScreen(navController) }

        // --- Manager -------------------------------------------------------
        composable(Routes.MANAGER_DASHBOARD) { ManagerDashboardScreen(navController) }
        composable(Routes.PROJECTS) { ProjectListScreen(navController) }
        composable(Routes.EMPLOYEES) { EmployeeListScreen(navController) }
        composable(Routes.INVENTORY) { InventoryScreen(navController) }
        composable(Routes.REPORTS) { ReportsScreen(navController) }

        // --- Administrator -------------------------------------------------
        composable(Routes.ADMIN_DASHBOARD) { AdminDashboardScreen(navController) }
        composable(Routes.USER_MANAGEMENT) { UserManagementScreen() }
        composable(Routes.AUDIT_LOG) { AuditLogScreen() }
        composable(Routes.SYSTEM_SETTINGS) {
            PlaceholderScreen(title = stringResource(R.string.title_system_settings))
        }

        // --- Detail routes -------------------------------------------------
        composable(
            route = Routes.TASK_DETAIL_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_TASK_ID) { type = NavType.StringType })
        ) { entry ->
            TaskDetailRoute(
                taskId = taskIdFrom(entry.arguments?.getString(Routes.ARG_TASK_ID)),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PROJECT_DETAIL_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_PROJECT_ID) { type = NavType.StringType })
        ) { entry ->
            ProjectDetailScreen(
                navController = navController,
                projectId = entry.arguments?.getString(Routes.ARG_PROJECT_ID).orEmpty()
            )
        }

        composable(
            route = Routes.EMPLOYEE_PROFILE_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_EMPLOYEE_ID) { type = NavType.StringType })
        ) { entry ->
            EmployeeProfileScreen(
                navController = navController,
                employeeId = entry.arguments?.getString(Routes.ARG_EMPLOYEE_ID).orEmpty()
            )
        }

        composable(
            route = Routes.MATERIAL_DETAIL_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_MATERIAL_ID) { type = NavType.StringType })
        ) { entry ->
            MaterialDetailScreen(
                navController = navController,
                materialId = entry.arguments?.getString(Routes.ARG_MATERIAL_ID).orEmpty()
            )
        }

        composable(
            route = Routes.CHAT_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_CONVERSATION_ID) { type = NavType.StringType })
        ) { entry ->
            ChatScreen(
                navController = navController,
                conversationId = entry.arguments?.getString(Routes.ARG_CONVERSATION_ID).orEmpty()
            )
        }
    }
}

/** Where opening a notification of [type] leads, for the signed-in [role]. */
private fun notificationRoute(type: NotificationType, role: UserRole?): String = when (type) {
    NotificationType.TASK -> Routes.TASKS
    NotificationType.SAFETY -> Routes.SAFETY_CHECKLIST
    NotificationType.MESSAGE -> Routes.MESSAGES
    NotificationType.INVENTORY -> Routes.INVENTORY
    NotificationType.REPORT -> if (role == UserRole.WORKER) Routes.DAILY_REPORT else Routes.REPORTS
    NotificationType.SYSTEM -> Routes.SYNC_STATUS
}

private val ProfileShortcut.route: String
    get() = when (this) {
        ProfileShortcut.ATTENDANCE -> Routes.ATTENDANCE
        ProfileShortcut.DAILY_REPORT -> Routes.DAILY_REPORT
        ProfileShortcut.SAFETY_CHECKLIST -> Routes.SAFETY_CHECKLIST
        ProfileShortcut.TASKS -> Routes.TASKS
        ProfileShortcut.EMPLOYEES -> Routes.EMPLOYEES
        ProfileShortcut.REPORTS -> Routes.REPORTS
        ProfileShortcut.SYSTEM_SETTINGS -> Routes.SYSTEM_SETTINGS
        ProfileShortcut.DOCUMENTS -> Routes.DOCUMENTS
        ProfileShortcut.NOTIFICATIONS -> Routes.NOTIFICATIONS
        ProfileShortcut.SYNC -> Routes.SYNC_STATUS
        ProfileShortcut.SETTINGS -> Routes.SETTINGS
    }

private val WorkerDestination.route: String
    get() = when (this) {
        WorkerDestination.TASKS -> Routes.TASKS
        WorkerDestination.ATTENDANCE -> Routes.ATTENDANCE
        WorkerDestination.DAILY_REPORT -> Routes.DAILY_REPORT
        WorkerDestination.SAFETY_CHECKLIST -> Routes.SAFETY_CHECKLIST
        WorkerDestination.INCIDENT_REPORT -> Routes.INCIDENT_REPORT
        WorkerDestination.NOTIFICATIONS -> Routes.NOTIFICATIONS
    }

/**
 * Task ids in routes are numbers. Older screens still pass the prototype's
 * "T-101" form; its digits are the same id, so both work.
 */
private fun taskIdFrom(argument: String?): Int =
    argument.orEmpty().filter(Char::isDigit).toIntOrNull() ?: -1

/** The inspection argument's value when an incident is reported on its own. */
private const val NO_INSPECTION = -1
