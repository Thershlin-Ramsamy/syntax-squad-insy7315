package com.mcc.construction.ui.screens.worker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

@Composable
fun DailyReportScreen(navController: NavHostController) {
    val user = MockRepository.currentUser
    val online by MockRepository.isOnline

    var hours by rememberSaveable { mutableStateOf("9") }
    var project by rememberSaveable { mutableStateOf("Riverside Estate") }
    var summary by rememberSaveable { mutableStateOf("") }
    var blockers by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            if (submitted) {
                Banner(
                    title = if (online) "Report submitted" else "Report queued",
                    message = if (online) {
                        "Your daily report has been sent to Naledi Khumalo for approval."
                    } else {
                        "You're offline. The report is saved on this device and will " +
                            "upload automatically when you reconnect."
                    },
                    icon = Icons.Filled.CloudOff,
                    tone = if (online) Tone.Success else Tone.Warning
                )
                Spacer(Modifier.height(Spacing.lg))
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                KeyValueRow("Date", "14 August 2026")
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Submitted by", user.name)
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Site", user.site)
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Project")
            Spacer(Modifier.height(Spacing.blockGap))
            SegmentedFilter(
                options = listOf("Riverside Estate", "Harbour View Offices"),
                selected = project,
                onSelect = { project = it }
            )

            Spacer(Modifier.height(Spacing.xl))

            AppTextField(
                value = hours,
                onValueChange = { hours = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = "Hours worked",
                placeholder = "9",
                keyboardType = KeyboardType.Number
            )

            Spacer(Modifier.height(Spacing.lg))

            AppTextField(
                value = summary,
                onValueChange = { summary = it },
                label = "Work completed",
                placeholder = "What did you get done today?",
                singleLine = false,
                minLines = 4
            )

            Spacer(Modifier.height(Spacing.lg))

            AppTextField(
                value = blockers,
                onValueChange = { blockers = it },
                label = "Delays or blockers",
                placeholder = "Anything that held work up (optional)",
                singleLine = false,
                minLines = 3
            )

            Spacer(Modifier.height(Spacing.xxl))

            PrimaryButton(
                text = if (online) "Submit report" else "Save and queue",
                icon = Icons.Filled.Send,
                onClick = { submitted = true },
                enabled = summary.isNotBlank() && hours.isNotBlank()
            )
            if (summary.isBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Add a short summary of the work completed to submit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
