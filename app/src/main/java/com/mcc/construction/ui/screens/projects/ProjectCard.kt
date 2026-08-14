package com.mcc.construction.ui.screens.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mcc.construction.data.Project
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Eyebrow
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.tone

/**
 * A project summary card.
 *
 * A plain [com.mcc.construction.ui.components.ListRow] can't carry a progress
 * track, and progress is the one figure that makes a portfolio scannable — so
 * projects get their own card shape. Shared between the manager dashboard and
 * the projects list so the two never drift apart.
 */
@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Eyebrow(project.client)
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = project.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(project.status.label, project.status.tone)
        }
        Spacer(Modifier.height(Spacing.md))
        ProgressTrack(project.progress, tone = project.status.tone)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "${(project.progress * 100).toInt()}% complete · " +
                "${project.openTasks} open tasks · ${project.teamSize} crew",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
