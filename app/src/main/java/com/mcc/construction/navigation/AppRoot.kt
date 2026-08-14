package com.mcc.construction.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.ui.components.AppTopBar

/**
 * Single NavHost and back stack for the whole app.
 *
 * Destinations listed in [NavMenus.chromeRoutes] render inside the adaptive
 * bottom-bar / rail / drawer shell. Everything else — splash, login, and every
 * pushed detail or form screen — takes the full window with its own back
 * affordance, which keeps modal-feeling flows from inheriting a tab bar.
 */
@Composable
fun AppRoot(widthSizeClass: WindowWidthSizeClass) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val role by MockRepository.currentRole

    val showChrome = currentRoute != null && currentRoute in NavMenus.chromeRoutes

    if (showChrome) {
        AdaptiveAppScaffold(
            widthSizeClass = widthSizeClass,
            role = role,
            currentRoute = currentRoute,
            onNavItemSelected = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            topBar = {
                AppTopBar(
                    currentRoute = currentRoute,
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) }
                )
            }
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
