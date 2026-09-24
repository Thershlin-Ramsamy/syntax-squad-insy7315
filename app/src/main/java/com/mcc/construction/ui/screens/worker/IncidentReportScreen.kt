package com.mcc.construction.ui.screens.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.IncidentSeverity
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.ChoiceChip
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PhotoAttachment
import com.mcc.construction.ui.components.PhotoAttachmentRow
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.rememberPhotoPicker
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Incident report wired to its ViewModel.
 *
 * @param inspectionId the failed checklist this report comes from, or null.
 */
@Composable
fun IncidentReportRoute(
    inspectionId: Int?,
    onDone: () -> Unit,
    viewModel: IncidentReportViewModel = viewModel(
        key = "incident-${inspectionId ?: "new"}",
        factory = IncidentReportViewModel.factory(inspectionId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickPhoto = rememberPhotoPicker(maxItems = 1) { uris ->
        uris.firstOrNull()?.let { viewModel.onPhotoPicked(it.toString()) }
    }
    IncidentReportScreen(
        state = state,
        onProjectSelected = viewModel::onProjectSelected,
        onSeveritySelected = viewModel::onSeveritySelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAddPhrase = viewModel::onAddPhrase,
        onAddPhoto = pickPhoto,
        onRemovePhoto = viewModel::onRemovePhoto,
        onSubmit = viewModel::onSubmit,
        onDone = onDone
    )
}

@Composable
fun IncidentReportScreen(
    state: IncidentReportUiState,
    onProjectSelected: (Int) -> Unit,
    onSeveritySelected: (IncidentSeverity) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddPhrase: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        modifier = modifier,
        title = stringResource(R.string.incident_title),
        onBack = onDone,
        maxContentWidth = if (LocalWindowLayout.current.isExpanded) WIDE_CONTENT else Spacing.contentMaxWidth
    ) {
        when {
            state.isLoading -> LoadingState()
            state.form.submitted != null -> Submitted(state, state.form.submitted, onDone)
            else -> Form(state, onProjectSelected, onSeveritySelected, onDescriptionChange, onAddPhrase, onAddPhoto, onRemovePhoto, onSubmit)
        }
    }
}

@Composable
private fun Form(
    state: IncidentReportUiState,
    onProjectSelected: (Int) -> Unit,
    onSeveritySelected: (IncidentSeverity) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddPhrase: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSubmit: () -> Unit
) {
    val inspection = state.linkedInspection
    if (inspection != null) {
        Banner(
            title = stringResource(R.string.incident_linked_title),
            message = stringResource(
                R.string.incident_linked_message,
                state.selectedProject?.projectName.orEmpty(),
                inspection.date.formatDate()
            ),
            icon = Icons.Filled.Link,
            tone = Tone.Info
        )
        Spacer(Modifier.height(Spacing.sectionGap))
    } else if (state.projects.size > 1) {
        SectionHeader(title = stringResource(R.string.checklist_site))
        Spacer(Modifier.height(Spacing.blockGap))
        ChoiceChipGroup(
            options = state.projects,
            selected = state.selectedProject,
            onSelect = { onProjectSelected(it.projectId) },
            label = { it.projectName }
        )
        Spacer(Modifier.height(Spacing.sectionGap))
    }

    SectionHeader(title = stringResource(R.string.incident_severity))
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = stringResource(R.string.incident_severity_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = if (IncidentField.SEVERITY in state.form.missing) Tone.Danger.colors().ink
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.blockGap))
    SeverityPicker(state.form.severity, onSeveritySelected)

    if (state.isUrgent) {
        Spacer(Modifier.height(Spacing.lg))
        Banner(
            title = stringResource(R.string.incident_urgent_title),
            message = stringResource(R.string.incident_urgent_message),
            icon = Icons.Filled.LocalHospital,
            tone = Tone.Danger
        )
    }

    Spacer(Modifier.height(Spacing.sectionGap))
    val descriptionMissing = IncidentField.DESCRIPTION in state.form.missing
    AppTextField(
        value = state.form.description,
        onValueChange = onDescriptionChange,
        label = stringResource(R.string.incident_description),
        placeholder = stringResource(R.string.incident_description_placeholder),
        singleLine = false,
        minLines = 4,
        isError = descriptionMissing,
        supportingText = if (descriptionMissing) stringResource(R.string.incident_description_missing) else null
    )
    Spacer(Modifier.height(Spacing.sm))
    QuickPhrases(state.form.description, onAddPhrase)

    Spacer(Modifier.height(Spacing.sectionGap))
    SectionHeader(title = stringResource(R.string.incident_photo))
    Spacer(Modifier.height(Spacing.blockGap))
    PhotoAttachmentRow(
        photos = listOfNotNull(state.form.photoUri).map { PhotoAttachment(it, it) },
        onAddPhoto = onAddPhoto,
        onRemovePhoto = { onRemovePhoto() },
        maxPhotos = 1
    )

    Spacer(Modifier.height(Spacing.sectionGap))
    if (state.form.missing.isNotEmpty()) {
        Banner(
            title = stringResource(R.string.report_missing_title),
            message = stringResource(R.string.incident_missing_message),
            icon = Icons.Filled.Error,
            tone = Tone.Danger
        )
        Spacer(Modifier.height(Spacing.md))
    }
    PrimaryButton(
        text = stringResource(R.string.incident_submit),
        icon = Icons.Filled.ReportProblem,
        loading = state.form.isSubmitting,
        onClick = onSubmit
    )
}

/** Five large buttons, 1 to 5, each with its number, colour and a word. */
@Composable
private fun SeverityPicker(selected: IncidentSeverity?, onSelect: (IncidentSeverity) -> Unit) {
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        IncidentSeverity.entries.forEach { severity ->
            val isSelected = severity == selected
            val c = severity.tone.colors()
            val label = stringResource(severity.labelRes)
            val shape = MaterialTheme.shapes.medium
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SEVERITY_HEIGHT)
                    .clip(shape)
                    .background(if (isSelected) c.ink else c.container)
                    .border(if (isSelected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(severity) })
                    .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val content = if (isSelected) MaterialTheme.colorScheme.surface else c.ink
                Text(
                    text = severity.level.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = content
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickPhrases(description: String, onAddPhrase: (String) -> Unit) {
    Text(
        text = stringResource(R.string.incident_quick_add),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.xs))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        QUICK_PHRASES.forEach { res ->
            val phrase = stringResource(res)
            ChoiceChip(
                label = phrase,
                selected = description.contains(phrase),
                onClick = { onAddPhrase(phrase) },
                icon = Icons.Filled.Add
            )
        }
    }
}

@Composable
private fun ColumnScope.Submitted(state: IncidentReportUiState, incident: IncidentReport, onDone: () -> Unit) {
    val offline = state.form.submittedOffline
    EmptyState(
        icon = if (offline) Icons.Filled.CloudOff else Icons.Filled.CheckCircle,
        title = stringResource(R.string.incident_sent_title),
        message = stringResource(
            if (offline) R.string.incident_sent_offline_message else R.string.incident_sent_message,
            stringResource(incident.severity.labelRes)
        ),
        tone = if (offline) Tone.Warning else Tone.Success
    )
    PrimaryButton(text = stringResource(R.string.report_done), onClick = onDone)
}

private val QUICK_PHRASES = listOf(
    R.string.incident_phrase_slip,
    R.string.incident_phrase_falling,
    R.string.incident_phrase_equipment,
    R.string.incident_phrase_injury,
    R.string.incident_phrase_no_injury
)

private val SEVERITY_HEIGHT: Dp = 72.dp
private val WIDE_CONTENT: Dp = 720.dp

private fun previewState(severity: IncidentSeverity?, linked: Boolean): IncidentReportUiState {
    val today = LocalDate.now()
    val project = Project(1, 2, "Riverside Estate Phase 2", "Centurion", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0)
    return IncidentReportUiState(
        isLoading = false,
        projects = listOf(project),
        linkedInspection = if (linked) SafetyInspection(9, 1, 5, today, "Failed: Scaffold tags current", false) else null,
        form = IncidentForm(projectId = 1, severity = severity)
    )
}

@DevicePreviews
@Composable
private fun IncidentReportPreview() {
    PreviewSurface {
        IncidentReportScreen(previewState(IncidentSeverity.MODERATE, linked = true), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@DevicePreviews
@Composable
private fun IncidentReportUrgentPreview() {
    PreviewSurface {
        IncidentReportScreen(previewState(IncidentSeverity.CRITICAL, linked = false), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@DevicePreviews
@Composable
private fun IncidentReportSentPreview() {
    val state = previewState(IncidentSeverity.MINOR, linked = false)
    PreviewSurface {
        IncidentReportScreen(
            state.copy(form = state.form.copy(submitted = IncidentReport(4, 9, 2, "Cut finger", LocalDateTime.now()))),
            {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
