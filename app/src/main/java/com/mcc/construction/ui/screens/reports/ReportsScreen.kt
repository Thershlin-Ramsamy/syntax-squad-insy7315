package com.mcc.construction.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Eyebrow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.MetricTile
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone

/**
 * Reports history.
 *
 * Two streams a manager reviews together: the daily reports crews submit, and
 * the safety incidents raised against the same sites. Approval state is the
 * scannable column on the left-hand stream, severity on the right-hand one.
 */
@Composable
fun ReportsScreen(navController: NavHostController) {
    val reports = MockRepository.dailyReports
    val incidents = MockRepository.incidents

    val approved = reports.count { it.status == "Approved" }
    val awaiting = reports.size - approved

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
                MetricTile(
                    value = approved.toString(),
                    label = "Approved",
                    icon = Icons.Filled.CheckCircle,
                    tone = Tone.Success,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = awaiting.toString(),
                    label = "Awaiting review",
                    icon = Icons.Filled.PendingActions,
                    tone = if (awaiting == 0) Tone.Neutral else Tone.Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(
                title = "Daily reports",
                actionLabel = "Submit",
                onAction = { navController.navigate(Routes.DAILY_REPORT) }
            )
            Spacer(Modifier.height(Spacing.blockGap))

            reports.forEachIndexed { index, report ->
                if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("${report.date} · ${report.project}")
                            Spacer(Modifier.height(Spacing.xxs))
                            Text(
                                text = report.submittedBy,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        StatusPill(
                            text = report.status,
                            tone = if (report.status == "Approved") Tone.Success else Tone.Warning
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = report.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "${report.hours} logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(
                title = "Incidents",
                actionLabel = "Report",
                onAction = { navController.navigate(Routes.INCIDENT_REPORT) }
            )
            Spacer(Modifier.height(Spacing.blockGap))

            incidents.forEachIndexed { index, incident ->
                if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                ListRow(
                    title = incident.title,
                    subtitle = "${incident.site} · ${incident.reportedBy}",
                    meta = incident.date,
                    leading = {
                        LeadingIcon(Icons.Filled.HealthAndSafety, incident.severity.tone)
                    },
                    trailing = {
                        StatusPill(incident.severity.label, incident.severity.tone)
                    }
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            SectionHeader(title = "Documents")
            Spacer(Modifier.height(Spacing.blockGap))
            ListRow(
                title = "Project documents",
                subtitle = "${MockRepository.documents.size} files across all sites",
                onClick = { navController.navigate(Routes.DOCUMENTS) },
                leading = { LeadingIcon(Icons.Filled.Description, Tone.Info) }
            )
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}
