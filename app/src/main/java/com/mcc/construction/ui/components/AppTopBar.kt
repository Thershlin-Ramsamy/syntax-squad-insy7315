package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mcc.construction.R
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing

/**
 * App bar for destinations inside the navigation chrome.
 *
 * Carries the sync indicator on every screen rather than only where it seems
 * relevant — an offline-first app has to make connection state ambient, not
 * something the user goes looking for.
 *
 * @param onBack shown when the screen was reached from another screen rather
 *   than picked from the menu (Settings opened from Profile, for example).
 * @param onProfile shown when Profile is not already in the role's menu, so
 *   every role can reach its account in one tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    unreadNotifications: Int,
    onNotifications: () -> Unit,
    onBack: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = {
            SyncStatusIndicator()
            NotificationsButton(unreadNotifications, onNotifications)
            if (onProfile != null) {
                IconButton(onClick = onProfile) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = stringResource(R.string.action_open_profile)
                    )
                }
            }
            Spacer(Modifier.width(Spacing.xs))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun NotificationsButton(unread: Int, onClick: () -> Unit) {
    val description = if (unread > 0) {
        pluralStringResource(R.plurals.notifications_unread_description, unread, unread)
    } else {
        stringResource(R.string.title_notifications)
    }
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (unread > 0) {
                    Badge { Text(if (unread > MAX_BADGE) "$MAX_BADGE+" else unread.toString()) }
                }
            }
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = description)
        }
    }
}

private const val MAX_BADGE = 9

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun AppTopBarPreview() {
    PreviewSurface {
        AppTopBar(
            title = "Dashboard",
            unreadNotifications = 3,
            onNotifications = {},
            onProfile = {}
        )
    }
}
