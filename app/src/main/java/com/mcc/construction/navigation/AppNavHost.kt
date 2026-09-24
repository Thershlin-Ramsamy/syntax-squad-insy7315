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
import com.mcc.construction.ui.components.PlaceholderScreen
import com.mcc.construction.ui.screens.admin.AdminDashboardScreen
import com.mcc.construction.ui.screens.admin.AuditLogScreen
import com.mcc.construction.ui.screens.admin.UserManagementScreen
import com.mcc.construction.ui.screens.dashboard.ManagerDashboardScreen
import com.mcc.construction.ui.screens.dashboard.WorkerDashboardScreen
import com.mcc.construction.ui.screens.documents.DocumentsScreen
import com.mcc.construction.ui.screens.employees.EmployeeListScreen
import com.mcc.construction.ui.screens.employees.EmployeeProfileScreen
import com.mcc.construction.ui.screens.inventory.InventoryScreen
import com.mcc.construction.ui.screens.inventory.MaterialDetailScreen
import com.mcc.construction.ui.screens.login.ForgotPasswordRoute
import com.mcc.construction.ui.screens.login.LoginRoute
import com.mcc.construction.ui.screens.messages.ChatScreen
import com.mcc.construction.ui.screens.messages.MessagesScreen
import com.mcc.construction.ui.screens.notifications.NotificationsScreen
import com.mcc.construction.ui.screens.profile.ProfileScreen
import com.mcc.construction.ui.screens.projects.ProjectDetailScreen
import com.mcc.construction.ui.screens.projects.ProjectListScreen
import com.mcc.construction.ui.screens.reports.ReportsScreen
import com.mcc.construction.ui.screens.settings.SettingsScreen
import com.mcc.construction.ui.screens.splash.SplashDestination
import com.mcc.construction.ui.screens.splash.SplashRoute
import com.mcc.construction.ui.screens.sync.SyncStatusScreen
import com.mcc.construction.ui.screens.tasks.TaskDetailScreen
import com.mcc.construction.ui.screens.tasks.TaskListScreen
import com.mcc.construction.ui.screens.worker.AttendanceScreen
import com.mcc.construction.ui.screens.worker.DailyReportScreen
import com.mcc.construction.ui.screens.worker.IncidentReportScreen
import com.mcc.construction.ui.screens.worker.SafetyChecklistScreen

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
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
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
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(navController) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.SYNC_STATUS) { SyncStatusScreen(navController) }
        composable(Routes.DOCUMENTS) { DocumentsScreen(navController) }

        // --- Worker --------------------------------------------------------
        composable(Routes.WORKER_DASHBOARD) { WorkerDashboardScreen(navController) }
        composable(Routes.TASKS) { TaskListScreen(navController) }
        composable(Routes.ATTENDANCE) { AttendanceScreen(navController) }
        composable(Routes.DAILY_REPORT) { DailyReportScreen(navController) }
        composable(Routes.SAFETY_CHECKLIST) { SafetyChecklistScreen(navController) }
        composable(Routes.INCIDENT_REPORT) { IncidentReportScreen(navController) }
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
            TaskDetailScreen(
                navController = navController,
                taskId = entry.arguments?.getString(Routes.ARG_TASK_ID).orEmpty()
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
