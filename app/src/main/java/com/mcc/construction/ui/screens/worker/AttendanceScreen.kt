package com.mcc.construction.ui.screens.worker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.ui.components.AccentButton
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Eyebrow
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.MetricInline
import com.mcc.construction.ui.components.MetricStrip
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

@Composable
fun AttendanceScreen(navController: NavHostController) {
    val clockedIn by MockRepository.isClockedIn
    val records = MockRepository.attendance

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            // Current state card — the answer to "am I on the clock?" has to be
            // the first thing on this screen, not a row in the history list.
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Eyebrow("Current status")
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = if (clockedIn) "On site" else "Not clocked in",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.sm))
                StatusPill(
                    text = if (clockedIn) "Since 06:52" else "Last out 16:12 yesterday",
                    tone = if (clockedIn) Tone.Success else Tone.Neutral,
                    showDot = true
                )
                Spacer(Modifier.height(Spacing.lg))
                MetricStrip {
                    MetricInline("38h", "This week")
                    MetricInline("9h 24m", "Yesterday")
                    MetricInline("5", "Days on site")
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            AccentButton(
                text = if (clockedIn) "Clock Out" else "Clock In",
                icon = if (clockedIn) Icons.AutoMirrored.Filled.Logout
                else Icons.AutoMirrored.Filled.Login,
                onClick = { MockRepository.toggleClockedIn() }
            )

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "This week")
            Spacer(Modifier.height(Spacing.blockGap))

            records.forEachIndexed { index, record ->
                if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                ListRow(
                    title = record.date,
                    subtitle = "${record.clockIn} – ${record.clockOut ?: "still on site"}",
                    meta = record.site,
                    leading = { LeadingIcon(Icons.Filled.AccessTime, Tone.Info) },
                    trailing = {
                        Text(
                            text = record.hours,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Week summary")
            Spacer(Modifier.height(Spacing.blockGap))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                KeyValueRow("Standard hours", "36h 00m")
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Overtime", "2h 00m")
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Total", "38h 00m")
            }
        }
    }
}
