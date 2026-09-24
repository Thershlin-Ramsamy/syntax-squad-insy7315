package com.mcc.construction.ui.screens.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.SafetyChecklistItem
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Safety checklist wired to its ViewModel.
 *
 * @param onReportIncident opens an incident report linked to the given inspection.
 */
@Composable
fun SafetyChecklistRoute(
    onReportIncident: (inspectionId: Int) -> Unit,
    onDone: () -> Unit,
    viewModel: SafetyChecklistViewModel = viewModel(factory = SafetyChecklistViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SafetyChecklistScreen(
        state = state,
        onProjectSelected = viewModel::onProjectSelected,
        onAnswer = viewModel::onAnswer,
        onNotesChange = viewModel::onNotesChange,
        onSubmit = viewModel::onSubmit,
        onStartAgain = viewModel::onStartAgain,
        onReportIncident = onReportIncident,
        onDone = onDone
    )
}

@Composable
fun SafetyChecklistScreen(
    state: SafetyChecklistUiState,
    onProjectSelected: (Int) -> Unit,
    onAnswer: (itemId: Int, isSafe: Boolean) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onStartAgain: () -> Unit,
    onReportIncident: (Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }
    val submitted = state.form.submitted
    val wide = LocalWindowLayout.current.isExpanded
    ScreenScaffold(modifier = modifier, maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth) {
        if (submitted != null) {
            SubmittedResult(state, submitted, onReportIncident, onStartAgain, onDone)
            return@ScreenScaffold
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                Column(Modifier.weight(1f)) {
                    Header(state, onProjectSelected)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    SubmitPanel(state, onNotesChange, onSubmit)
                }
                Column(Modifier.weight(1.3f)) { Items(state, onAnswer) }
            }
        } else {
            Header(state, onProjectSelected)
            Spacer(Modifier.height(Spacing.sectionGap))
            Items(state, onAnswer)
            Spacer(Modifier.height(Spacing.sectionGap))
            SubmitPanel(state, onNotesChange, onSubmit)
        }
    }
}

@Composable
private fun Header(state: SafetyChecklistUiState, onProjectSelected: (Int) -> Unit) {
    state.lastToday?.let { last ->
        Banner(
            title = stringResource(if (last.passed) R.string.checklist_done_today_pass else R.string.checklist_done_today_fail),
            message = stringResource(R.string.checklist_done_today_message),
            icon = Icons.Filled.Info,
            tone = if (last.passed) Tone.Success else Tone.Warning
        )
        Spacer(Modifier.height(Spacing.lg))
    }
    if (state.projects.size > 1) {
        SectionHeader(title = stringResource(R.string.checklist_site))
        Spacer(Modifier.height(Spacing.blockGap))
        ChoiceChipGroup(
            options = state.projects,
            selected = state.selectedProject,
            onSelect = { onProjectSelected(it.projectId) },
            label = { it.projectName }
        )
        Spacer(Modifier.height(Spacing.lg))
    } else {
        Text(
            text = state.selectedProject?.projectName.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))
    }
    ProgressSummary(state)
}

/** "4 of 6 checked" with a bar and the result so far. */
@Composable
private fun ProgressSummary(state: SafetyChecklistUiState) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.checklist_progress, state.answeredCount, state.items.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            ResultChip(state.result)
        }
        Spacer(Modifier.height(Spacing.md))
        ProgressTrack(
            progress = if (state.items.isEmpty()) 0f else state.answeredCount / state.items.size.toFloat(),
            tone = when (state.result) {
                ChecklistResult.FAIL -> Tone.Danger
                ChecklistResult.PASS -> Tone.Success
                ChecklistResult.INCOMPLETE -> Tone.Info
            }
        )
    }
}

@Composable
private fun ResultChip(result: ChecklistResult) {
    when (result) {
        ChecklistResult.PASS -> StatusChip(stringResource(R.string.checklist_result_pass), Tone.Success, icon = Icons.Filled.CheckCircle)
        ChecklistResult.FAIL -> StatusChip(stringResource(R.string.checklist_result_fail), Tone.Danger, icon = Icons.Filled.Dangerous)
        ChecklistResult.INCOMPLETE -> StatusChip(stringResource(R.string.checklist_result_incomplete), Tone.Neutral)
    }
}

@Composable
private fun Items(state: SafetyChecklistUiState, onAnswer: (Int, Boolean) -> Unit) {
    SectionHeader(title = stringResource(R.string.checklist_items))
    Spacer(Modifier.height(Spacing.blockGap))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        state.items.forEachIndexed { index, item ->
            ChecklistItemCard(index + 1, item, state.form.answers[item.itemId], onAnswer)
        }
    }
}

/** One check with two large answer buttons, each a full half of the card's width. */
@Composable
private fun ChecklistItemCard(
    number: Int,
    item: SafetyChecklistItem,
    answer: Boolean?,
    onAnswer: (Int, Boolean) -> Unit
) {
    val outline = when (answer) {
        true -> Tone.Success.colors().figure
        false -> Tone.Danger.colors().figure
        null -> MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(if (answer == null) 1.dp else 2.dp, outline, MaterialTheme.shapes.medium)
            .padding(Spacing.cardPad)
    ) {
        Text(
            text = stringResource(R.string.checklist_item_title, number, item.title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = item.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AnswerButton(stringResource(R.string.checklist_yes), Icons.Filled.Check, Tone.Success,
                selected = answer == true, onClick = { onAnswer(item.itemId, true) }, modifier = Modifier.weight(1f))
            AnswerButton(stringResource(R.string.checklist_no), Icons.Filled.Close, Tone.Danger,
                selected = answer == false, onClick = { onAnswer(item.itemId, false) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AnswerButton(
    label: String,
    icon: ImageVector,
    tone: Tone,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = tone.colors()
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = modifier
            .heightIn(min = Spacing.minTouch)
            .clip(shape)
            // The darker ink colour, not the bright figure colour, so white text
            // on a selected answer keeps AA contrast in sunlight.
            .background(if (selected) c.ink else MaterialTheme.colorScheme.surface)
            .border(if (selected) 0.dp else 1.dp, MaterialTheme.colorScheme.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val content = if (selected) MaterialTheme.colorScheme.surface else c.ink
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.titleMedium, color = content)
    }
}

@Composable
private fun SubmitPanel(state: SafetyChecklistUiState, onNotesChange: (String) -> Unit, onSubmit: () -> Unit) {
    AppTextField(
        value = state.form.notes,
        onValueChange = onNotesChange,
        label = stringResource(R.string.checklist_notes),
        placeholder = stringResource(R.string.checklist_notes_placeholder),
        singleLine = false,
        minLines = 2
    )
    Spacer(Modifier.height(Spacing.lg))
    if (!state.canSubmit) {
        Text(
            text = stringResource(R.string.checklist_answer_all, state.items.size - state.answeredCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))
    }
    PrimaryButton(
        text = stringResource(R.string.checklist_submit),
        icon = Icons.Filled.HealthAndSafety,
        enabled = state.canSubmit,
        loading = state.form.isSubmitting,
        onClick = onSubmit
    )
}

@Composable
private fun ColumnScope.SubmittedResult(
    state: SafetyChecklistUiState,
    inspection: SafetyInspection,
    onReportIncident: (Int) -> Unit,
    onStartAgain: () -> Unit,
    onDone: () -> Unit
) {
    val offlineNote = if (state.form.submittedOffline) " " + stringResource(R.string.checklist_saved_offline) else ""
    if (inspection.passed) {
        EmptyState(
            icon = if (state.form.submittedOffline) Icons.Filled.CloudOff else Icons.Filled.CheckCircle,
            title = stringResource(R.string.checklist_passed_title),
            message = stringResource(R.string.checklist_passed_message) + offlineNote,
            tone = Tone.Success
        )
        PrimaryButton(text = stringResource(R.string.report_done), onClick = onDone)
    } else {
        EmptyState(
            icon = Icons.Filled.Dangerous,
            title = stringResource(R.string.checklist_failed_title),
            message = stringResource(
                R.string.checklist_failed_message,
                state.failedItems.joinToString { it.title }
            ) + offlineNote,
            tone = Tone.Danger
        )
        // One tap from a failed check to reporting it — the incident is linked to this inspection.
        PrimaryButton(
            text = stringResource(R.string.checklist_report_incident),
            icon = Icons.Filled.ReportProblem,
            onClick = { onReportIncident(inspection.inspectionId) }
        )
        Spacer(Modifier.height(Spacing.md))
        SecondaryButton(
            text = stringResource(R.string.report_done),
            onClick = onDone,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
    Spacer(Modifier.height(Spacing.md))
    SecondaryButton(
        text = stringResource(R.string.checklist_start_again),
        icon = Icons.Filled.Replay,
        onClick = onStartAgain,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}

private val WIDE_CONTENT: Dp = 1120.dp

private fun previewState(answers: Map<Int, Boolean>): SafetyChecklistUiState {
    val today = LocalDate.now()
    return SafetyChecklistUiState(
        isLoading = false,
        items = listOf(
            SafetyChecklistItem(1, "PPE inspected", "Hard hat, boots, hi-vis and gloves checked and undamaged"),
            SafetyChecklistItem(2, "Work area cordoned off", "Barriers and signage in place around the pour zone"),
            SafetyChecklistItem(3, "Scaffold tags current", "Every scaffold bay shows a green tag dated this week")
        ),
        projects = listOf(Project(1, 2, "Riverside Estate Phase 2", "Centurion", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0)),
        form = ChecklistForm(projectId = 1, answers = answers)
    )
}

@DevicePreviews
@Composable
private fun SafetyChecklistPreview() {
    PreviewSurface {
        SafetyChecklistScreen(previewState(mapOf(1 to true, 2 to false)), {}, { _, _ -> }, {}, {}, {}, {}, {})
    }
}

@DevicePreviews
@Composable
private fun SafetyChecklistFailedPreview() {
    val state = previewState(mapOf(1 to true, 2 to false, 3 to true))
    PreviewSurface {
        SafetyChecklistScreen(
            state.copy(form = state.form.copy(submitted = SafetyInspection(9, 1, 5, LocalDate.now(), "", false))),
            {}, { _, _ -> }, {}, {}, {}, {}, {}
        )
    }
}
