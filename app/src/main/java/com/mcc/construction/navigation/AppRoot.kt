package com.mcc.construction.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mcc.construction.ui.components.AppTopBar

/**
 * Single NavHost/back stack for the whole app. Screens in [Routes.mainRoutes] are
 * wrapped in the adaptive bottom-bar/rail chrome; Splash, Login and Task Details
 * render full-screen with no chrome.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppRoot(widthSizeClass: WindowWidthSizeClass) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute != null && Routes.mainRoutes.contains(currentRoute)

    if (showChrome) {
        AdaptiveAppScaffold(
            widthSizeClass = widthSizeClass,
            currentRoute = currentRoute,
            onNavItemSelected = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            topBar = { AppTopBar(currentRoute) }
        ) { padding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        }
    } else {
        AppNavHost(navController = navController, modifier = Modifier.fillMaxSize())
    }
}
