package com.mcc.construction.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

// Same item set drives both the compact bottom bar and the expanded rail —
// only the container chrome changes with window size, per CLAUDE.md.
val mainNavItems = listOf(
    NavItem(Routes.HOME, "Home", Icons.Filled.Home),
    NavItem(Routes.PROJECTS, "Projects", Icons.Filled.FolderOpen),
    NavItem(Routes.TASKS, "Tasks", Icons.Filled.Checklist),
    NavItem(Routes.MESSAGES, "Messages", Icons.Filled.ChatBubble),
    NavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
)
