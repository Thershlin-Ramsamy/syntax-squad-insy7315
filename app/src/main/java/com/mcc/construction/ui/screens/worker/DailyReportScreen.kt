package com.mcc.construction.ui.screens.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.Weather
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
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.rememberPhotoPicker
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import java.math.BigDecimal
import java.time.LocalDate

/** Daily report wired to its ViewModel. [onDone] leaves the screen after a report is sent. */
@Composable
fun DailyReportRoute(
    onDone: () -> Unit,
    viewModel: DailyReportViewModel = viewModel(factory = DailyReportViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickPhotos = rememberPhotoPicker(maxItems = DailyReportViewModel.MAX_PHOTOS) { uris ->
        viewModel.onPhotosPicked(uris.map { it.toString() })
    }
    DailyReportScreen(
        state = state,
        actions = DailyReportActions(
            onProjectSelected = viewModel::onProjectSelected,
            onWeatherSelected = viewModel::onWeatherSelected,
            onWorkCompletedChange = viewModel::onWorkCompletedChange,
            onProblemsChange = viewModel::onProblemsChange,
            onAddFinishedTasks = viewModel::onAddFinishedTasks,
            onAddProblem = viewModel::onAddProblem,
            onAddPhoto = pickPhotos,
            onRemovePhoto = viewModel::onRemovePhoto,
            onSubmitClick = viewModel::onSubmitClick,
            onConfirmSubmit = viewModel::onConfirmSubmit,
            onDismissConfirm = viewModel::onDismissConfirm,
            onStartAnother = viewModel::onStartAnother,
            onDone = onDone
        )
    )
}

/** Everything the daily report screen can ask its ViewModel to do. */
data class DailyReportActions(
    val onProjectSelected: (Int) -> Unit = {},
    val onWeatherSelected: (Weather) -> Unit = {},
    val onWorkCompletedChange: (String) -> Unit = {},
    val onProblemsChange: (String) -> Unit = {},
    val onAddFinishedTasks: () -> Unit = {},
    val onAddProblem: (String) -> Unit = {},
    val onAddPhoto: () -> Unit = {},
    val onRemovePhoto: (String) -> Unit = {},
    val onSubmitClick: () -> Unit = {},
    val onConfirmSubmit: () -> Unit = {},
    val onDismissConfirm: () -> Unit = {},
    val onStartAnother: () -> Unit = {},
    val onDone: () -> Unit = {}
)

@Composable
fun DailyReportScreen(
    state: DailyReportUiState,
    actions: DailyReportActions,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }
    val submitted = state.form.submitted
    if (submitted != null) {
        ScreenScaffold(modifier = modifier) {
            SubmittedConfirmation(state, actions)
        }
        return
    }
    val wide = LocalWindowLayout.current.isExpanded
    ScreenScaffold(modifier = modifier, maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth) {
        state.alreadySubmittedToday?.let {
            Banner(
                title = stringResource(R.string.report_already_sent_title),
                message = stringResource(R.string.report_already_sent_message),
                icon = Icons.Filled.Info,
                tone = Tone.Info
            )
            Spacer(Modifier.height(Spacing.lg))
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                Column(Modifier.weight(1f)) {
                    ProjectAndWeather(state, actions)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Photos(state, actions)
                }
                Column(Modifier.weight(1f)) {
                    WorkAndProblems(state, actions)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    SubmitButton(state, actions)
                }
            }
        } else {
            ProjectAndWeather(state, actions)
            Spacer(Modifier.height(Spacing.sectionGap))
            WorkAndProblems(state, actions)
            Spacer(Modifier.height(Spacing.sectionGap))
            Photos(state, actions)
            Spacer(Modifier.height(Spacing.sectionGap))
            SubmitButton(state, actions)
        }
    }

    if (state.form.isConfirmVisible) {
        AlertDialog(
            onDismissRequest = actions.onDismissConfirm,
            icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
            iconContentColor = MaterialTheme.colorScheme.primary,
            title = { Text(stringResource(R.string.report_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.report_confirm_message,
                        state.selectedProject?.projectName.orEmpty(),
                        state.form.photoUris.size
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = actions.onConfirmSubmit) { Text(stringResource(R.string.report_confirm_send)) }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissConfirm) { Text(stringResource(R.string.report_confirm_edit)) }
            }
        )
    }
}

@Composable
private fun ProjectAndWeather(state: DailyReportUiState, actions: DailyReportActions) {
    FieldHeader(R.string.report_project, missing = ReportField.PROJECT in state.form.missing)
    ChoiceChipGroup(
        options = state.projects,
        selected = state.selectedProject,
        onSelect = { actions.onProjectSelected(it.projectId) },
        label = { it.projectName }
    )
    Spacer(Modifier.height(Spacing.sectionGap))
    FieldHeader(R.string.report_weather, missing = ReportField.WEATHER in state.form.missing)
    ChoiceChipGroup(
        options = Weather.entries,
        selected = state.form.weather,
        onSelect = actions.onWeatherSelected,
        label = { stringResource(it.labelRes) },
        icon = { it.icon }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkAndProblems(state: DailyReportUiState, actions: DailyReportActions) {
    val workMissing = ReportField.WORK_COMPLETED in state.form.missing
    AppTextField(
        value = state.form.workCompleted,
        onValueChange = actions.onWorkCompletedChange,
        label = stringResource(R.string.report_work_completed),
        placeholder = stringResource(R.string.report_work_placeholder),
        singleLine = false,
        minLines = 4,
        isError = workMissing,
        supportingText = if (workMissing) stringResource(R.string.report_missing_work) else null
    )
    val notYetAdded = state.finishedTaskTitles.filterNot { state.form.workCompleted.contains(it) }
    if (notYetAdded.isNotEmpty()) {
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton(
            text = pluralStringResource(R.plurals.report_add_finished_tasks, notYetAdded.size, notYetAdded.size),
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            onClick = actions.onAddFinishedTasks
        )
    }

    Spacer(Modifier.height(Spacing.sectionGap))
    AppTextField(
        value = state.form.problems,
        onValueChange = actions.onProblemsChange,
        label = stringResource(R.string.report_problems),
        placeholder = stringResource(R.string.report_problems_placeholder),
        singleLine = false,
        minLines = 3
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = stringResource(R.string.report_quick_add),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.xs))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        QUICK_PROBLEMS.forEach { res ->
            val phrase = stringResource(res)
            ChoiceChip(
                label = phrase,
                selected = state.form.problems.contains(phrase),
                onClick = { actions.onAddProblem(phrase) },
                icon = Icons.Filled.Add
            )
        }
    }
}

@Composable
private fun Photos(state: DailyReportUiState, actions: DailyReportActions) {
    SectionHeader(title = stringResource(R.string.report_photos))
    Spacer(Modifier.height(Spacing.blockGap))
    PhotoAttachmentRow(
        photos = state.form.photoUris.map { PhotoAttachment(it, it) },
        onAddPhoto = actions.onAddPhoto,
        onRemovePhoto = { actions.onRemovePhoto(it.uri) },
        maxPhotos = DailyReportViewModel.MAX_PHOTOS
    )
}

@Composable
private fun SubmitButton(state: DailyReportUiState, actions: DailyReportActions) {
    if (state.form.missing.isNotEmpty()) {
        Banner(
            title = stringResource(R.string.report_missing_title),
            message = stringResource(R.string.report_missing_message),
            icon = Icons.Filled.Error,
            tone = Tone.Danger
        )
        Spacer(Modifier.height(Spacing.md))
    }
    PrimaryButton(
        text = stringResource(R.string.report_submit),
        icon = Icons.AutoMirrored.Filled.Send,
        loading = state.form.isSubmitting,
        onClick = actions.onSubmitClick
    )
}

/** A section heading that turns red with a prompt when the field is still missing. */
@Composable
private fun FieldHeader(titleRes: Int, missing: Boolean) {
    SectionHeader(title = stringResource(titleRes))
    if (missing) {
        Text(
            text = stringResource(R.string.report_pick_one),
            style = MaterialTheme.typography.bodyMedium,
            color = Tone.Danger.colors().ink
        )
    }
    Spacer(Modifier.height(Spacing.blockGap))
}

@Composable
private fun ColumnScope.SubmittedConfirmation(state: DailyReportUiState, actions: DailyReportActions) {
    val offline = state.form.submittedOffline
    EmptyState(
        icon = if (offline) Icons.Filled.CloudOff else Icons.Filled.CheckCircle,
        title = stringResource(R.string.report_sent_title),
        message = stringResource(
            if (offline) R.string.report_sent_offline_message else R.string.report_sent_message,
            state.selectedProject?.projectName.orEmpty()
        ),
        tone = if (offline) Tone.Warning else Tone.Success
    )
    PrimaryButton(text = stringResource(R.string.report_done), onClick = actions.onDone)
    Spacer(Modifier.height(Spacing.md))
    SecondaryButton(
        text = stringResource(R.string.report_write_another),
        icon = Icons.Filled.Description,
        onClick = actions.onStartAnother,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}

private val QUICK_PROBLEMS = listOf(
    R.string.report_problem_none,
    R.string.report_problem_materials,
    R.string.report_problem_weather,
    R.string.report_problem_equipment
)

private val WIDE_CONTENT: Dp = 1080.dp

private fun previewState(): DailyReportUiState {
    val today = LocalDate.now()
    val project = Project(1, 2, "Riverside Estate Phase 2", "Centurion, Gauteng", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0)
    return DailyReportUiState(
        isLoading = false,
        projects = listOf(project, project.copy(projectId = 2, projectName = "Harbour View Offices")),
        form = DailyReportForm(projectId = 1, weather = Weather.SUNNY, workCompleted = "Morning safety walkaround"),
        finishedTaskTitles = listOf("Morning safety walkaround")
    )
}

@DevicePreviews
@Composable
private fun DailyReportPreview() {
    PreviewSurface { DailyReportScreen(previewState(), DailyReportActions()) }
}

@DevicePreviews
@Composable
private fun DailyReportSubmittedPreview() {
    val state = previewState()
    PreviewSurface {
        DailyReportScreen(
            state.copy(
                form = state.form.copy(
                    submitted = DailyReport(9, 1, 5, LocalDate.now(), "Sunny", "Work", "", ReportStatus.SUBMITTED)
                )
            ),
            DailyReportActions()
        )
    }
}
