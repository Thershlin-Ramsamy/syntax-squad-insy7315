package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.theme.Spacing

/**
 * App bar for destinations inside the navigation chrome.
 *
 * Carries the sync chip on every screen rather than only where it seems
 * relevant — an offline-first app has to make connection state ambient, not
 * something the user goes looking for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    currentRoute: String?,
    onNotifications: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = titleFor(currentRoute),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SyncStatusChip()
                IconButton(onClick = onNotifications) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
                Spacer(Modifier.width(Spacing.xs))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(horizontal = Spacing.xs)
    )
}

private fun titleFor(route: String?): String = when (route) {
    Routes.WORKER_DASHBOARD -> "Dashboard"
    Routes.MANAGER_DASHBOARD -> "Dashboard"
    Routes.ADMIN_DASHBOARD -> "Administration"
    Routes.TASKS -> "Tasks"
    Routes.ATTENDANCE -> "Attendance"
    Routes.MESSAGES -> "Messages"
    Routes.PROFILE -> "Profile"
    Routes.SETTINGS -> "Settings"
    Routes.NOTIFICATIONS -> "Notifications"
    Routes.DAILY_REPORT -> "Daily Report"
    Routes.SAFETY_CHECKLIST -> "Safety Checklist"
    Routes.DOCUMENTS -> "Documents"
    Routes.PROJECTS -> "Projects"
    Routes.EMPLOYEES -> "Employees"
    Routes.INVENTORY -> "Inventory"
    Routes.REPORTS -> "Reports"
    Routes.USER_MANAGEMENT -> "User Management"
    Routes.AUDIT_LOG -> "Audit Log"
    else -> "MCC Construction"
}
