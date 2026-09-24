package com.mcc.construction.ui.screens.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

/**
 * The screens a role reaches from Profile. On a phone the bottom bar holds four
 * or five destinations, so everything else the role can open is listed here —
 * the same destinations the wider rail shows directly.
 */
enum class ProfileShortcut(
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
    val tone: Tone
) {
    ATTENDANCE(R.string.nav_attendance, Icons.Filled.AccessTime, Tone.Info),
    DAILY_REPORT(R.string.nav_daily_report, Icons.Filled.Description, Tone.Info),
    SAFETY_CHECKLIST(R.string.title_safety_checklist, Icons.Filled.HealthAndSafety, Tone.Warning),
    TASKS(R.string.nav_tasks, Icons.AutoMirrored.Filled.Assignment, Tone.Info),
    EMPLOYEES(R.string.nav_employees, Icons.Filled.Groups, Tone.Info),
    REPORTS(R.string.nav_reports, Icons.Filled.Assessment, Tone.Info),
    SYSTEM_SETTINGS(R.string.title_system_settings, Icons.Filled.Tune, Tone.Neutral),
    DOCUMENTS(R.string.nav_documents, Icons.Filled.FolderOpen, Tone.Info),
    NOTIFICATIONS(R.string.title_notifications, Icons.Filled.Notifications, Tone.Warning),
    SYNC(R.string.title_sync_status, Icons.Filled.CloudSync, Tone.Neutral),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Tone.Neutral);

    companion object {
        fun forRole(role: UserRole): List<ProfileShortcut> {
            val roleSpecific = when (role) {
                UserRole.WORKER -> listOf(ATTENDANCE, DAILY_REPORT, SAFETY_CHECKLIST, DOCUMENTS)
                UserRole.MANAGER -> listOf(TASKS, DOCUMENTS, SAFETY_CHECKLIST)
                UserRole.ADMINISTRATOR -> listOf(SYSTEM_SETTINGS, EMPLOYEES, REPORTS)
            }
            return roleSpecific + listOf(NOTIFICATIONS, SYNC, SETTINGS)
        }
    }
}

/** Profile wired to its ViewModel. */
@Composable
fun ProfileRoute(
    onShortcut: (ProfileShortcut) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSignedOut) {
        if (state.isSignedOut) onSignedOut()
    }

    ProfileScreen(
        state = state,
        onShortcut = onShortcut,
        onSignOutClick = viewModel::onSignOutClick,
        onSignOutConfirm = viewModel::onSignOutConfirm,
        onSignOutDismiss = viewModel::onSignOutDismiss
    )
}

/**
 * The signed-in user's details, their role, shortcuts to the rest of the app,
 * and sign-out. On a wide window the details and the shortcuts sit side by side.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onShortcut: (ProfileShortcut) -> Unit,
    onSignOutClick: () -> Unit,
    onSignOutConfirm: () -> Unit,
    onSignOutDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = state.user
    if (user == null) {
        LoadingState(modifier)
        return
    }

    ScreenScaffold(modifier = modifier, maxContentWidth = Spacing.contentMaxWidth) {
        if (LocalWindowLayout.current.isExpanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                Column(Modifier.weight(1f)) {
                    Identity(user)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    SignOutButton(onSignOutClick)
                }
                Column(Modifier.weight(1f)) {
                    Shortcuts(user.role, state, onShortcut)
                }
            }
        } else {
            Identity(user)
            Spacer(Modifier.height(Spacing.sectionGap))
            Shortcuts(user.role, state, onShortcut)
            Spacer(Modifier.height(Spacing.sectionGap))
            SignOutButton(onSignOutClick)
        }
    }

    if (state.isSignOutDialogVisible) {
        AlertDialog(
            onDismissRequest = onSignOutDismiss,
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            iconContentColor = MaterialTheme.colorScheme.primary,
            title = { Text(stringResource(R.string.profile_sign_out_title)) },
            text = {
                Text(
                    if (state.pendingChanges > 0) {
                        pluralStringResource(
                            R.plurals.profile_sign_out_pending,
                            state.pendingChanges,
                            state.pendingChanges
                        )
                    } else {
                        stringResource(R.string.profile_sign_out_message)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onSignOutConfirm) {
                    Text(stringResource(R.string.profile_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = onSignOutDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun Identity(user: User) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(user.fullName, size = 72.dp)
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = user.jobTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            StatusChip(
                text = stringResource(user.role.labelRes),
                tone = Tone.Info,
                icon = user.role.icon
            )
        }
    }
    Spacer(Modifier.height(Spacing.xl))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        InfoRow(Icons.Filled.Badge, stringResource(R.string.profile_employee_number), user.employeeNumber)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Email, stringResource(R.string.profile_email), user.email)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Phone, stringResource(R.string.profile_phone), user.phone)
    }
}

@Composable
private fun Shortcuts(
    role: UserRole,
    state: ProfileUiState,
    onShortcut: (ProfileShortcut) -> Unit
) {
    SectionHeader(title = stringResource(R.string.profile_more))
    Spacer(Modifier.height(Spacing.blockGap))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        ProfileShortcut.forRole(role).forEach { shortcut ->
            ListRow(
                title = stringResource(shortcut.titleRes),
                subtitle = shortcutSubtitle(shortcut, state),
                onClick = { onShortcut(shortcut) },
                leading = { LeadingIcon(shortcut.icon, shortcut.tone) },
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun shortcutSubtitle(shortcut: ProfileShortcut, state: ProfileUiState): String? =
    when (shortcut) {
        ProfileShortcut.NOTIFICATIONS -> if (state.unreadNotifications > 0) {
            pluralStringResource(
                R.plurals.profile_unread,
                state.unreadNotifications,
                state.unreadNotifications
            )
        } else {
            stringResource(R.string.profile_all_read)
        }

        ProfileShortcut.SYNC -> if (state.pendingChanges > 0) {
            pluralStringResource(
                R.plurals.profile_pending_uploads,
                state.pendingChanges,
                state.pendingChanges
            )
        } else {
            stringResource(R.string.profile_everything_uploaded)
        }

        else -> null
    }

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    SecondaryButton(
        text = stringResource(R.string.profile_sign_out),
        icon = Icons.AutoMirrored.Filled.Logout,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    )
}

private val previewUser = User(
    userId = 5,
    role = UserRole.WORKER,
    firstName = "Sipho",
    lastName = "Dlamini",
    email = "worker@mcc.co.za",
    phone = "+27 82 445 1120",
    jobTitle = "Site Operative",
    employeeNumber = "MCC-2041"
)

@DevicePreviews
@Composable
private fun ProfileScreenPreview() {
    PreviewSurface {
        ProfileScreen(
            state = ProfileUiState(user = previewUser, unreadNotifications = 2, pendingChanges = 3),
            onShortcut = {},
            onSignOutClick = {},
            onSignOutConfirm = {},
            onSignOutDismiss = {}
        )
    }
}

@DevicePreviews
@Composable
private fun ProfileSignOutDialogPreview() {
    PreviewSurface {
        ProfileScreen(
            state = ProfileUiState(user = previewUser, pendingChanges = 2, isSignOutDialogVisible = true),
            onShortcut = {},
            onSignOutClick = {},
            onSignOutConfirm = {},
            onSignOutDismiss = {}
        )
    }
}
