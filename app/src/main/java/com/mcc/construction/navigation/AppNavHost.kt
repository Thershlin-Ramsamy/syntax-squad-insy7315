package com.mcc.construction.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mcc.construction.ui.screens.dashboard.WorkerDashboardScreen
import com.mcc.construction.ui.screens.login.LoginScreen
import com.mcc.construction.ui.screens.placeholder.ComingSoonScreen
import com.mcc.construction.ui.screens.splash.SplashScreen
import com.mcc.construction.ui.screens.tasks.TaskDetailScreen
import com.mcc.construction.ui.screens.tasks.TaskListScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.HOME) { WorkerDashboardScreen(navController) }
        composable(Routes.TASKS) { TaskListScreen(navController) }
        composable(Routes.PROJECTS) { ComingSoonScreen("Projects") }
        composable(Routes.MESSAGES) { ComingSoonScreen("Messages") }
        composable(Routes.PROFILE) { ComingSoonScreen("Profile") }
        composable(
            route = Routes.TASK_DETAIL_PATTERN,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
            TaskDetailScreen(navController, taskId)
        }
    }
}
