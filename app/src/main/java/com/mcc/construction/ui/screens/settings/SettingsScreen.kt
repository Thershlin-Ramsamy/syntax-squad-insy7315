package com.mcc.construction.ui.screens.settings

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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.data.legacy.label
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

/**
 * Settings.
 *
 * Toggles are local screen state rather than persisted preferences — Task 1 has
 * no storage layer, and a switch that visibly moves demonstrates the interface
 * without pretending to a durability the prototype does not have.
 */
@Composable
fun SettingsScreen(navController: NavHostController) {
    var largeText by rememberSaveable { mutableStateOf(true) }
    var highContrast by rememberSaveable { mutableStateOf(false) }
    var pushAlerts by rememberSaveable { mutableStateOf(true) }
    var vibrate by rememberSaveable { mutableStateOf(true) }
    var syncOnWifiOnly by rememberSaveable { mutableStateOf(false) }

    val online by MockRepository.isOnline
    val pending by MockRepository.pendingSyncCount

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            SectionHeader(title = "Display")
            Spacer(Modifier.height(Spacing.blockGap))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SettingSwitch(
                    icon = Icons.Filled.TextFields,
                    title = "Larger text",
                    subtitle = "Easier to read in bright sunlight",
                    checked = largeText,
                    onCheckedChange = { largeText = it }
                )
                Spacer(Modifier.height(Spacing.lg))
                SettingSwitch(
                    icon = Icons.Filled.DarkMode,
                    title = "High contrast",
                    subtitle = "Stronger borders and darker text",
                    checked = highContrast,
                    onCheckedChange = { highContrast = it }
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Notifications")
            Spacer(Modifier.height(Spacing.blockGap))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SettingSwitch(
                    icon = Icons.Filled.Notifications,
                    title = "Push alerts",
                    subtitle = "Task assignments and safety notices",
                    checked = pushAlerts,
                    onCheckedChange = { pushAlerts = it }
                )
                Spacer(Modifier.height(Spacing.lg))
                SettingSwitch(
                    icon = Icons.Filled.Vibration,
                    title = "Vibrate",
                    subtitle = "Useful when ear protection is worn",
                    checked = vibrate,
                    onCheckedChange = { vibrate = it }
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Data & sync")
            Spacer(Modifier.height(Spacing.blockGap))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SettingSwitch(
                    icon = Icons.Filled.DataUsage,
                    title = "Sync on Wi-Fi only",
                    subtitle = "Hold uploads until the site office network is in range",
                    checked = syncOnWifiOnly,
                    onCheckedChange = { syncOnWifiOnly = it }
                )
            }
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "Offline & sync status",
                subtitle = if (online) "Online · $pending queued" else "Offline · $pending queued",
                onClick = { navController.navigate(Routes.SYNC_STATUS) },
                leading = {
                    LeadingIcon(
                        Icons.Filled.CloudSync,
                        if (online) Tone.Success else Tone.Danger
                    )
                }
            )

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "About")
            Spacer(Modifier.height(Spacing.blockGap))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                KeyValueRow("App version", "1.0 (prototype)")
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Signed in as", MockRepository.currentUser.role.label)
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Data source", "Mock — no backend")
            }
            Spacer(Modifier.height(Spacing.itemGap))
            ListRow(
                title = "MCC Construction",
                subtitle = "Mobile Management System · INSY7315 prototype",
                leading = { LeadingIcon(Icons.Filled.Info, Tone.Neutral) }
            )
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}

/** Icon, label and switch on one row — the repeated unit of this screen. */
@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadingIcon(icon, Tone.Neutral)
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
