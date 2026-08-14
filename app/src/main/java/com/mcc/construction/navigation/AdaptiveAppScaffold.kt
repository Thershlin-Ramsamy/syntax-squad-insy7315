package com.mcc.construction.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Below the medium breakpoint: bottom navigation bar (compact/phone).
 * At or above the medium breakpoint: navigation rail (expanded/tablet-desktop).
 * Same nav items and same NavHost content either way — only the chrome shape changes.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AdaptiveAppScaffold(
    widthSizeClass: WindowWidthSizeClass,
    currentRoute: String?,
    onNavItemSelected: (String) -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    if (widthSizeClass == WindowWidthSizeClass.Compact) {
        Scaffold(
            topBar = topBar,
            bottomBar = {
                NavigationBar {
                    mainNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { onNavItemSelected(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding -> content(padding) }
    } else {
        Row(Modifier.fillMaxSize()) {
            NavigationRail {
                Spacer(Modifier.height(12.dp))
                mainNavItems.forEach { item ->
                    NavigationRailItem(
                        selected = currentRoute == item.route,
                        onClick = { onNavItemSelected(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = topBar
            ) { padding -> content(padding) }
        }
    }
}
