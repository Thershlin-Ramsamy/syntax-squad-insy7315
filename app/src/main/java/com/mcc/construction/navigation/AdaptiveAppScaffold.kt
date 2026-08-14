package com.mcc.construction.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mcc.construction.data.UserRole
import com.mcc.construction.ui.theme.Spacing

/**
 * One content tree, three navigation chromes, chosen purely from window width:
 *
 *  - Compact  → bottom navigation bar, five destinations
 *  - Medium   → navigation rail, fuller menu
 *  - Expanded → permanent 260dp drawer with labelled rows, fuller menu
 *
 * The three-tier split follows the adaptive guidance in CLAUDE.md and the common
 * desktop-dashboard convention of a 240–280dp labelled sidebar: at expanded
 * width an icon rail wastes the horizontal space that makes a sidebar readable,
 * while at medium width a full drawer would crowd the content.
 *
 * No screen is duplicated per size class — only this wrapper changes.
 */
@Composable
fun AdaptiveAppScaffold(
    widthSizeClass: WindowWidthSizeClass,
    role: UserRole,
    currentRoute: String?,
    onNavItemSelected: (String) -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> Scaffold(
            topBar = topBar,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavMenus.compact(role).forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { onNavItemSelected(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            },
            content = content
        )

        WindowWidthSizeClass.Medium -> Row(Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(Spacing.md))
                NavMenus.expanded(role).forEach { item ->
                    NavigationRailItem(
                        selected = currentRoute == item.route,
                        onClick = { onNavItemSelected(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = {
                            Text(
                                item.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
                Spacer(Modifier.height(Spacing.md))
            }
            Scaffold(modifier = Modifier.weight(1f), topBar = topBar, content = content)
        }

        else -> Row(Modifier.fillMaxSize()) {
            PermanentDrawer(
                role = role,
                currentRoute = currentRoute,
                onNavItemSelected = onNavItemSelected
            )
            Scaffold(modifier = Modifier.weight(1f), topBar = topBar, content = content)
        }
    }
}

@Composable
private fun PermanentDrawer(
    role: UserRole,
    currentRoute: String?,
    onNavItemSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.lg, horizontal = Spacing.md)
    ) {
        // Brand lockup — the drawer is wide enough to carry it, so the app bar
        // does not have to repeat the product name at this size.
        Row(
            modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    text = "MCC Construction",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = role.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        NavMenus.expanded(role).forEach { item ->
            val selected = currentRoute == item.route
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                    .clickable { onNavItemSelected(item.route) }
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(Spacing.xs))
        }
    }
}
