package com.mcc.construction.ui.screens.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.ChecklistItem
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

@Composable
fun SafetyChecklistScreen(navController: NavHostController) {
    val items = MockRepository.checklist
    val done = items.count { it.checked }
    val allDone = done == items.size

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenPadding {
            Text(
                text = "$done of ${items.size} complete",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.md))
            ProgressTrack(
                progress = done.toFloat() / items.size,
                tone = if (allDone) Tone.Success else Tone.Warning
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "Complete every item before starting work on site today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.sectionGap))
            SectionHeader(title = "Pre-work checks")
            Spacer(Modifier.height(Spacing.blockGap))

            items.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(Spacing.itemGap))
                ChecklistRow(item) { MockRepository.toggleChecklistItem(item.id) }
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            if (allDone) {
                Banner(
                    title = "Checklist complete",
                    message = "Submit to log it against today's shift.",
                    icon = Icons.Filled.HealthAndSafety,
                    tone = Tone.Success
                )
                Spacer(Modifier.height(Spacing.lg))
            }

            PrimaryButton(
                text = "Submit checklist",
                onClick = { navController.popBackStack() },
                enabled = allDone
            )
            if (!allDone) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "${items.size - done} item(s) still outstanding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Whole row is the touch target rather than just a checkbox — the design notes call
 * for controls usable in gloves, and a 20dp checkbox is not.
 */
@Composable
private fun ChecklistRow(item: ChecklistItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onToggle)
            .padding(Spacing.cardPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    if (item.checked) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = if (item.checked) 0.dp else 1.5.dp,
                    color = if (item.checked) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(9.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.checked) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = item.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
