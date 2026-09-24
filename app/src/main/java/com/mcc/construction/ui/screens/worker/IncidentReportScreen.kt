package com.mcc.construction.ui.screens.worker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.IncidentSeverity
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.DetailScaffold
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

@Composable
fun IncidentReportScreen(navController: NavHostController) {
    var severity by rememberSaveable { mutableStateOf(IncidentSeverity.NEAR_MISS.label) }
    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    DetailScaffold(title = "Report an incident", onBack = { navController.popBackStack() }) {
        if (submitted) {
            EmptyState(
                icon = Icons.Filled.CheckCircle,
                title = "Incident reported",
                message = "Reference INC-2026-041. Zanele Mahlangu has been notified and " +
                    "will follow up with you on site.",
                tone = Tone.Success,
                actionLabel = "Done",
                onAction = { navController.popBackStack() }
            )
            return@DetailScaffold
        }

        ScreenPadding {
            Banner(
                title = "If anyone is hurt, get help first",
                message = "Call the site first aider on 072 555 0111 before filling this in.",
                icon = Icons.Filled.WarningAmber,
                tone = Tone.Danger
            )

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Severity")
            Spacer(Modifier.height(Spacing.blockGap))
            SegmentedFilter(
                options = IncidentSeverity.entries.map { it.label },
                selected = severity,
                onSelect = { severity = it }
            )

            Spacer(Modifier.height(Spacing.xl))

            AppTextField(
                value = title,
                onValueChange = { title = it },
                label = "What happened",
                placeholder = "Short description"
            )

            Spacer(Modifier.height(Spacing.lg))

            AppTextField(
                value = location,
                onValueChange = { location = it },
                label = "Where",
                placeholder = "e.g. Block C, Unit 4"
            )

            Spacer(Modifier.height(Spacing.lg))

            AppTextField(
                value = description,
                onValueChange = { description = it },
                label = "Details",
                placeholder = "What led up to it, who was involved, what was done",
                singleLine = false,
                minLines = 5
            )

            Spacer(Modifier.height(Spacing.xxl))

            PrimaryButton(
                text = "Submit report",
                icon = Icons.Filled.Send,
                onClick = { submitted = true },
                enabled = title.isNotBlank() && description.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Reports go to the site safety officer immediately, including " +
                    "when you're offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
