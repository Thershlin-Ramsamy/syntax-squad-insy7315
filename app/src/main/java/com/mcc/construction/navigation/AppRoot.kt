package com.mcc.construction.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.adaptive.WindowWidthClass
import com.mcc.construction.ui.adaptive.rememberWindowLayout
import com.mcc.construction.ui.components.AppTopBar
import com.mcc.construction.ui.components.LocalSyncIndicatorState
import com.mcc.construction.ui.components.SyncIndicatorState

/**
 * The app shell: one [NavigationSuiteScaffold] and one NavHost for the whole app.
 *
 * The window size picks the navigation component — a bottom bar on a compact
 * (phone) window, a navigation rail on anything wider — and the signed-in
 * role picks the items. Screens are written once and never know which chrome
 * surrounds them; the ones that change layout on wide windows read
 * [LocalWindowLayout].
 *
 * Sign-in, detail pages and forms hide the chrome (NavigationSuiteType.None)
 * and draw their own app bar with a back button, so modal-feeling flows do not
 * inherit a tab bar. The NavHost stays in the same place in the tree either
 * way, so switching chrome on and off never rebuilds the back stack.
 *
 * @param forcedWidthClass overrides the measured width. Only the screenshot
 *   tooling sets this.
 */
@Composable
fun AppRoot(
    forcedWidthClass: WindowWidthClass? = null,
    viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
) {
    val windowLayout = rememberWindowLayout(forcedWidthClass)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val role = state.role

    val showChrome = role != null && currentRoute in NavMenus.chromeRoutes
    val navItems = if (role == null) {
        emptyList()
    } else {
        NavMenus.visibleItems(role, windowLayout.isCompact, windowLayout.heightDp)
    }
    val navigationType = when {
        !showChrome -> NavigationSuiteType.None
        windowLayout.widthClass == WindowWidthClass.Compact -> NavigationSuiteType.NavigationBar
        else -> NavigationSuiteType.NavigationRail
    }

    val syncIndicator = remember(state.syncStatus, navController) {
        SyncIndicatorState(state.syncStatus) {
            navController.navigate(Routes.SYNC_STATUS) { launchSingleTop = true }
        }
    }

    val itemColors = navigationItemColors()

    SignedOutGuard(state.isSignedIn, currentRoute, navController, viewModel::hasActiveSession)
    LegacyStateBridge(role, state.syncStatus.isOnline)

    CompositionLocalProvider(
        LocalWindowLayout provides windowLayout,
        LocalSyncIndicatorState provides syncIndicator
    ) {
        NavigationSuiteScaffold(
            layoutType = navigationType,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.surface,
                navigationRailContainerColor = MaterialTheme.colorScheme.surface
            ),
            navigationSuiteItems = {
                navItems.forEach { item ->
                    item(
                        selected = currentRoute == item.route,
                        onClick = { navController.selectMenuItem(item.route, role) },
                        // The label below already names the item, so the icon
                        // is decorative for screen readers.
                        icon = { Icon(item.icon, contentDescription = null) },
                        colors = itemColors,
                        label = {
                            Text(
                                text = stringResource(item.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (showChrome) {
                        val inMenu = navItems.any { it.route == currentRoute }
                        val canGoBack = !inMenu && navController.previousBackStackEntry != null
                        AppTopBar(
                            title = stringResource(NavMenus.titleRes(currentRoute)),
                            unreadNotifications = state.unreadNotifications,
                            onNotifications = {
                                navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true }
                            },
                            onBack = if (canGoBack) ({ navController.popBackStack() }) else null,
                            onProfile = if (
                                currentRoute != Routes.PROFILE &&
                                navItems.none { it.route == Routes.PROFILE }
                            ) {
                                { navController.navigate(Routes.PROFILE) { launchSingleTop = true } }
                            } else {
                                null
                            }
                        )
                    }
                },
                // Full-window screens handle their own insets.
                contentWindowInsets = if (showChrome) ScaffoldDefaults.contentWindowInsets else WindowInsets(0)
            ) { padding ->
                AppNavHost(
                    navController = navController,
                    role = role,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                )
            }
        }
    }
}

/**
 * Navy for the selected item on both the bar and the rail. The Material
 * default uses the secondary colour, which here is the brand gold — about 2:1
 * against white, unreadable outdoors.
 */
@Composable
private fun navigationItemColors(): NavigationSuiteItemColors {
    val scheme = MaterialTheme.colorScheme
    return NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = scheme.onPrimaryContainer,
            selectedTextColor = scheme.primary,
            indicatorColor = scheme.primaryContainer,
            unselectedIconColor = scheme.onSurfaceVariant,
            unselectedTextColor = scheme.onSurfaceVariant
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = scheme.onPrimaryContainer,
            selectedTextColor = scheme.primary,
            indicatorColor = scheme.primaryContainer,
            unselectedIconColor = scheme.onSurfaceVariant,
            unselectedTextColor = scheme.onSurfaceVariant
        )
    )
}

/**
 * Handles a tap on a bar or rail item. Tapping the section you are already in
 * goes back to its top (Settings opened from Profile, then Profile tapped,
 * returns to Profile); tapping another section switches to it.
 */
private fun NavHostController.selectMenuItem(route: String, role: UserRole?) {
    // getBackStackEntry throws when the route is not on the back stack.
    val inCurrentSection = runCatching { getBackStackEntry(route) }.isSuccess
    if (inCurrentSection) {
        popBackStack(route, inclusive = false)
    } else {
        navigateToTopLevel(route, role)
    }
}

/**
 * Switches between menu destinations the standard way: one copy of each, with
 * each one's scroll position and sub-pages kept. Everything is popped back to
 * the role's home, which sits at the bottom of the stack after sign-in.
 */
private fun NavHostController.navigateToTopLevel(route: String, role: UserRole?) {
    val home = role?.let(NavMenus::homeRoute)
    navigate(route) {
        if (home != null) popUpTo(home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Goes to sign-in with nothing behind it: the back stack is cleared, and so is
 * the saved state of every menu destination, so the next person to sign in
 * never lands on the previous user's screens.
 */
fun NavHostController.navigateToSignIn() {
    NavMenus.chromeRoutes.forEach { clearBackStack(it) }
    navigate(Routes.LOGIN) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/**
 * Sends the user back to sign-in if a signed-in screen is showing with no
 * session — for example after the process is restarted and the in-memory
 * session is gone, but the navigation back stack was restored.
 */
@Composable
private fun SignedOutGuard(
    isSignedIn: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    hasActiveSession: () -> Boolean
) {
    LaunchedEffect(isSignedIn, currentRoute) {
        if (currentRoute != null && currentRoute !in Routes.public && !hasActiveSession()) {
            navController.navigateToSignIn()
        }
    }
}

/**
 * Keeps the legacy MockRepository's session role and connection flag in step
 * with the repositories, so screens not yet migrated show the signed-in user
 * and the real sync state. Delete once no screen reads MockRepository.
 */
@Composable
private fun LegacyStateBridge(role: UserRole?, isOnline: Boolean) {
    SideEffect {
        if (role != null) MockRepository.currentRole.value = role
        MockRepository.isOnline.value = isOnline
    }
}
