package com.mcc.construction.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Notification
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.format.relativeTime
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone
import java.time.LocalDateTime

/** Notifications wired to its ViewModel. [onOpen] takes the user to what the notification is about. */
@Composable
fun NotificationsRoute(
    onOpen: (NotificationType) -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationsScreen(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onNotificationClick = { notification ->
            viewModel.onNotificationOpened(notification)
            onOpen(notification.type)
        },
        onMarkAsRead = viewModel::onMarkAsRead,
        onMarkAllAsRead = viewModel::onMarkAllAsRead
    )
}

/**
 * The signed-in user's notifications, newest first. Unread ones stand out
 * three ways — a navy outline on a white card, a bold heading and a dot — so
 * they read at a glance and without relying on colour.
 */
@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onFilterChange: (NotificationFilter) -> Unit,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (Int) -> Unit,
    onMarkAllAsRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(modifier = modifier, scrollable = false) {
        if (state.notifications == null) {
            LoadingState()
            return@ScreenScaffold
        }

        Header(state, onFilterChange, onMarkAllAsRead)
        Spacer(Modifier.height(Spacing.blockGap))

        if (state.visible.isEmpty()) {
            val onlyUnread = state.filter == NotificationFilter.UNREAD && state.notifications.isNotEmpty()
            EmptyState(
                icon = Icons.Filled.NotificationsNone,
                title = stringResource(
                    if (onlyUnread) R.string.notifications_caught_up_title
                    else R.string.notifications_empty_title
                ),
                message = stringResource(
                    if (onlyUnread) R.string.notifications_caught_up_message
                    else R.string.notifications_empty_message
                ),
                tone = if (onlyUnread) Tone.Success else Tone.Neutral
            )
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            items(state.visible, key = { it.notificationId }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = { onNotificationClick(notification) },
                    onMarkAsRead = { onMarkAsRead(notification.notificationId) }
                )
            }
        }
    }
}

@Composable
private fun Header(
    state: NotificationsUiState,
    onFilterChange: (NotificationFilter) -> Unit,
    onMarkAllAsRead: () -> Unit
) {
    val allLabel = stringResource(R.string.notifications_filter_all)
    val unreadLabel = stringResource(R.string.notifications_filter_unread, state.unreadCount)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedFilter(
            options = listOf(allLabel, unreadLabel),
            selected = if (state.filter == NotificationFilter.ALL) allLabel else unreadLabel,
            onSelect = { onFilterChange(if (it == allLabel) NotificationFilter.ALL else NotificationFilter.UNREAD) },
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onMarkAllAsRead,
            enabled = state.unreadCount > 0,
            modifier = Modifier.heightIn(min = Spacing.touchTarget)
        ) {
            Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.notifications_mark_all_read), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    val unread = !notification.isRead
    val unreadDescription = stringResource(R.string.notifications_unread)
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (unread) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                }
            )
            .clickable(onClick = onClick)
            .semantics { if (unread) stateDescription = unreadDescription }
            .heightIn(min = Spacing.rowHeight)
            .padding(start = Spacing.cardPad, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
        verticalAlignment = Alignment.Top
    ) {
        LeadingIcon(notification.type.icon, notification.type.tone)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(notification.type.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unread) {
                    Spacer(Modifier.width(Spacing.sm))
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = notification.messageContent,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unread) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = relativeTime(notification.sentDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (unread) {
            IconButton(onClick = onMarkAsRead) {
                Icon(
                    Icons.Filled.MarkEmailRead,
                    contentDescription = stringResource(R.string.notifications_mark_read),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Spacer(Modifier.width(Spacing.md))
        }
    }
}

private val previewNow = LocalDateTime.now()

private val previewNotifications = listOf(
    Notification(1, 5, NotificationType.TASK, "Johan Botha assigned you “Electrical conduit rough-in — Unit 2”.", previewNow.minusMinutes(12), false),
    Notification(2, 5, NotificationType.SAFETY, "Your daily safety checklist for Riverside Estate Phase 2 is due.", previewNow.minusMinutes(64), false),
    Notification(3, 5, NotificationType.MESSAGE, "Johan Botha: Confirm the rebar spacing before you pour.", previewNow.minusHours(2), true),
    Notification(4, 5, NotificationType.SYSTEM, "4 changes saved offline were uploaded.", previewNow.minusDays(1), true)
)

@DevicePreviews
@Composable
private fun NotificationsScreenPreview() {
    PreviewSurface {
        NotificationsScreen(
            state = NotificationsUiState(notifications = previewNotifications),
            onFilterChange = {},
            onNotificationClick = {},
            onMarkAsRead = {},
            onMarkAllAsRead = {}
        )
    }
}

@DevicePreviews
@Composable
private fun NotificationsCaughtUpPreview() {
    PreviewSurface {
        NotificationsScreen(
            state = NotificationsUiState(
                notifications = previewNotifications.map { it.copy(isRead = true) },
                filter = NotificationFilter.UNREAD
            ),
            onFilterChange = {},
            onNotificationClick = {},
            onMarkAsRead = {},
            onMarkAllAsRead = {}
        )
    }
}
