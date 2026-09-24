package com.mcc.construction.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.data.legacy.label
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

/**
 * Profile, and the phone's route to everything the rail shows directly.
 *
 * On a compact window the bottom bar carries five destinations, so the
 * secondary ones — documents, safety, sync, settings — are reachable from here
 * rather than being unavailable. They are the same routes the expanded rail
 * links to, not a parallel set of screens.
 */
@Composable
fun ProfileScreen(navController: NavHostController) {
    val user = MockRepository.currentUser

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(user.name, size = 64.dp)
                Spacer(Modifier.width(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = user.name,
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
                    StatusPill(user.role.label, Tone.Info)
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(Icons.Filled.Badge, "Employee ID", user.employeeId)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Place, "Site", user.site)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Email, "Email", user.email)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Phone, "Phone", user.phone)
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "More")
            Spacer(Modifier.height(Spacing.blockGap))

            ListRow(
                title = "Documents",
                subtitle = "Drawings, method statements, safety file",
                onClick = { navController.navigate(Routes.DOCUMENTS) },
                leading = { LeadingIcon(Icons.Filled.FolderOpen, Tone.Info) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Notifications",
                subtitle = "${MockRepository.notifications.count { !it.read }} unread",
                onClick = { navController.navigate(Routes.NOTIFICATIONS) },
                leading = { LeadingIcon(Icons.Filled.Notifications, Tone.Warning) }
            )
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Offline & sync",
                subtitle = "${MockRepository.pendingSyncCount.intValue} items waiting to upload",
                onClick = { navController.navigate(Routes.SYNC_STATUS) },
                leading = { LeadingIcon(Icons.Filled.CloudSync, Tone.Neutral) }
            )

            if (user.role == UserRole.WORKER) {
                Spacer(Modifier.height(Spacing.itemGap))
                ListRow(
                    title = "Safety checklist",
                    subtitle = "Today's site safety sign-off",
                    onClick = { navController.navigate(Routes.SAFETY_CHECKLIST) },
                    leading = { LeadingIcon(Icons.Filled.HealthAndSafety, Tone.Warning) }
                )
                Spacer(Modifier.height(Spacing.itemGap))
                ListRow(
                    title = "Daily report",
                    subtitle = "Log today's hours and work done",
                    onClick = { navController.navigate(Routes.DAILY_REPORT) },
                    leading = { LeadingIcon(Icons.Filled.Description, Tone.Info) }
                )
            }

            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Settings",
                subtitle = "Appearance, alerts, data usage",
                onClick = { navController.navigate(Routes.SETTINGS) },
                leading = { LeadingIcon(Icons.Filled.Settings, Tone.Neutral) }
            )

            Spacer(Modifier.height(Spacing.sectionGap))

            SecondaryButton(
                text = "Sign out",
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = {
                    navController.navigate(Routes.LOGIN) {
                        // Pop the graph itself: signing out must not leave a
                        // previous role's screens reachable with the back button,
                        // and by this point splash and login have both been
                        // popped, so there is no earlier route to pop up to.
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}
