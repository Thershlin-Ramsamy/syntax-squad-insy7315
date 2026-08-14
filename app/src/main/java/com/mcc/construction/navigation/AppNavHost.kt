package com.mcc.construction.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mcc.construction.ui.screens.dashboard.ManagerDashboardScreen
import com.mcc.construction.ui.screens.dashboard.WorkerDashboardScreen
import com.mcc.construction.ui.screens.employees.EmployeeListScreen
import com.mcc.construction.ui.screens.employees.EmployeeProfileScreen
import com.mcc.construction.ui.screens.inventory.InventoryScreen
import com.mcc.construction.ui.screens.inventory.MaterialDetailScreen
import com.mcc.construction.ui.screens.login.ForgotPasswordScreen
import com.mcc.construction.ui.screens.login.LoginScreen
import com.mcc.construction.ui.screens.messages.ChatScreen
import com.mcc.construction.ui.screens.messages.MessagesScreen
import com.mcc.construction.ui.screens.placeholder.ComingSoonScreen
import com.mcc.construction.ui.screens.projects.ProjectDetailScreen
import com.mcc.construction.ui.screens.projects.ProjectListScreen
import com.mcc.construction.ui.screens.reports.ReportsScreen
import com.mcc.construction.ui.screens.splash.SplashScreen
import com.mcc.construction.ui.screens.tasks.TaskDetailScreen
import com.mcc.construction.ui.screens.tasks.TaskListScreen
import com.mcc.construction.ui.screens.worker.AttendanceScreen
import com.mcc.construction.ui.screens.worker.DailyReportScreen
import com.mcc.construction.ui.screens.worker.IncidentReportScreen
import com.mcc.construction.ui.screens.worker.SafetyChecklistScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        // --- Entry ---------------------------------------------------------
        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.FORGOT_PASSWORD) { ForgotPasswordScreen(navController) }

        // --- Shared --------------------------------------------------------
        composable(Routes.NOTIFICATIONS) { ComingSoonScreen("Notifications") }
        composable(Routes.PROFILE) { ComingSoonScreen("Profile") }
        composable(Routes.SETTINGS) { ComingSoonScreen("Settings") }
        composable(Routes.SYNC_STATUS) { ComingSoonScreen("Sync Status") }
        composable(Routes.DOCUMENTS) { ComingSoonScreen("Documents") }

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
        composable(Routes.ADMIN_DASHBOARD) { ComingSoonScreen("Admin Dashboard") }
        composable(Routes.USER_MANAGEMENT) { ComingSoonScreen("User Management") }
        composable(Routes.AUDIT_LOG) { ComingSoonScreen("Audit Log") }

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
