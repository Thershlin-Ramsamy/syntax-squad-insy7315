package com.mcc.construction.ui.screens.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.data.legacy.NotificationKind
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

@Composable
fun NotificationsScreen(navController: NavHostController) {
    val notifications = MockRepository.notifications

    if (notifications.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.NotificationsNone,
            title = "Nothing new",
            message = "Task assignments and safety notices will appear here."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screenH,
            end = Spacing.screenH,
            top = Spacing.lg,
            bottom = Spacing.xxxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        items(notifications, key = { it.id }) { notification ->
            ListRow(
                title = notification.title,
                subtitle = notification.body,
                meta = notification.timestamp,
                onClick = { navController.navigate(notification.kind.destination) },
                leading = {
                    LeadingIcon(notification.kind.icon, notification.kind.tone)
                },
                trailing = {
                    if (!notification.read) StatusPill("New", Tone.Info, showDot = true)
                }
            )
        }
    }
}

private val NotificationKind.icon: ImageVector
    get() = when (this) {
        NotificationKind.TASK -> Icons.AutoMirrored.Filled.Assignment
        NotificationKind.SAFETY -> Icons.Filled.HealthAndSafety
        NotificationKind.MESSAGE -> Icons.Filled.ChatBubble
        NotificationKind.SYSTEM -> Icons.Filled.Settings
        NotificationKind.INVENTORY -> Icons.Filled.Inventory2
    }

private val NotificationKind.tone: Tone
    get() = when (this) {
        NotificationKind.TASK -> Tone.Info
        NotificationKind.SAFETY -> Tone.Warning
        NotificationKind.MESSAGE -> Tone.Info
        NotificationKind.SYSTEM -> Tone.Neutral
        NotificationKind.INVENTORY -> Tone.Danger
    }

/** Where tapping a notification of this kind should land. */
private val NotificationKind.destination: String
    get() = when (this) {
        NotificationKind.TASK -> Routes.TASKS
        NotificationKind.SAFETY -> Routes.SAFETY_CHECKLIST
        NotificationKind.MESSAGE -> Routes.MESSAGES
        NotificationKind.SYSTEM -> Routes.SYNC_STATUS
        NotificationKind.INVENTORY -> Routes.INVENTORY
    }
