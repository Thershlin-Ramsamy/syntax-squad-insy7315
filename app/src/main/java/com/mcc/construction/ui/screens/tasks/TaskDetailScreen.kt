package com.mcc.construction.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PhotoAttachment
import com.mcc.construction.ui.components.PhotoAttachmentRow
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.components.rememberPhotoPicker
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors
import com.mcc.construction.ui.tone
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Task details wired to their ViewModel.
 *
 * @param embedded true when shown in the task list's side pane on a wide
 *   window: no app bar of its own, and the action button sits in the content.
 */
@Composable
fun TaskDetailRoute(
    taskId: Int,
    onBack: () -> Unit,
    embedded: Boolean = false,
    viewModel: TaskDetailViewModel = viewModel(
        key = "task-detail-$taskId",
        factory = TaskDetailViewModel.factory(taskId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickPhotos = rememberPhotoPicker(maxItems = TaskDetailViewModel.MAX_PHOTOS) { uris ->
        viewModel.onPhotosPicked(uris.map { it.toString() })
    }
    TaskDetailScreen(
        state = state,
        embedded = embedded,
        onBack = onBack,
        onAdvanceStatus = viewModel::onAdvanceStatus,
        onAddPhoto = pickPhotos,
        onRemovePhoto = viewModel::onRemovePhoto,
        onCompleteWithoutPhoto = viewModel::onCompleteWithoutPhoto,
        onDismissCompleteDialog = viewModel::onDismissCompleteDialog,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
fun TaskDetailScreen(
    state: TaskDetailUiState,
    embedded: Boolean,
    onBack: () -> Unit,
    onAdvanceStatus: () -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onCompleteWithoutPhoto: () -> Unit,
    onDismissCompleteDialog: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(R.string.task_detail_title)
    val hasActionBar = !embedded && state.task != null && state.canUpdate
    ActionFeedbackHost(
        message = state.message,
        onMessageShown = onMessageShown,
        modifier = modifier,
        bottomInset = if (hasActionBar) ACTION_BAR_HEIGHT else 0.dp
    ) {
        ScreenScaffold(
            title = if (embedded) null else title,
            onBack = if (embedded) null else onBack,
            bottomBar = {
                if (!embedded) ActionBar(state, onAdvanceStatus)
            }
        ) {
            when {
                state.isLoading -> LoadingState()
                state.isNotFound -> EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = stringResource(R.string.task_not_found_title),
                    message = stringResource(R.string.task_not_found_message)
                )
                else -> {
                    TaskDetailContent(state, onAddPhoto, onRemovePhoto)
                    if (embedded) {
                        Spacer(Modifier.height(Spacing.sectionGap))
                        AdvanceButton(state, onAdvanceStatus)
                    }
                }
            }
        }
    }

    if (state.isCompleteWithoutPhotoDialogVisible) {
        AlertDialog(
            onDismissRequest = onDismissCompleteDialog,
            icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
            iconContentColor = MaterialTheme.colorScheme.primary,
            title = { Text(stringResource(R.string.task_no_photo_title)) },
            text = { Text(stringResource(R.string.task_no_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDismissCompleteDialog()
                    onAddPhoto()
                }) { Text(stringResource(R.string.task_no_photo_add)) }
            },
            dismissButton = {
                TextButton(onClick = onCompleteWithoutPhoto) {
                    Text(stringResource(R.string.task_no_photo_complete))
                }
            }
        )
    }
}

@Composable
private fun TaskDetailContent(
    state: TaskDetailUiState,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit
) {
    val task = state.task ?: return
    Text(
        text = task.title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(Spacing.md))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatusChip(stringResource(task.status.labelRes), task.status.tone)
        if (task.priority == TaskPriority.HIGH) {
            StatusChip(stringResource(task.priority.labelRes), task.priority.tone)
        }
    }

    Spacer(Modifier.height(Spacing.xl))
    StatusSteps(task.status)

    if (task.status == TaskStatus.BLOCKED) {
        Spacer(Modifier.height(Spacing.lg))
        Banner(
            title = stringResource(R.string.task_blocked_title),
            message = stringResource(R.string.task_blocked_message),
            icon = Icons.Filled.Block,
            tone = Tone.Danger
        )
    } else if (!state.canUpdate) {
        Spacer(Modifier.height(Spacing.lg))
        Banner(
            title = stringResource(R.string.task_read_only_title),
            message = stringResource(R.string.task_read_only_message),
            icon = Icons.Filled.Group,
            tone = Tone.Neutral
        )
    }

    Spacer(Modifier.height(Spacing.sectionGap))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        InfoRow(Icons.Filled.Work, stringResource(R.string.task_detail_project), state.project?.projectName.orEmpty())
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Place, stringResource(R.string.task_detail_location), state.project?.location.orEmpty())
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Event, stringResource(R.string.task_detail_deadline), deadlineText(task.deadline, state.today, task.status))
        Spacer(Modifier.height(Spacing.md))
        InfoRow(
            Icons.Filled.Group,
            stringResource(R.string.task_detail_assigned),
            state.assignees.joinToString { it.fullName }.ifEmpty { stringResource(R.string.task_detail_unassigned) }
        )
    }

    // Instructions are optional when a manager assigns a task.
    if (task.description.isNotBlank()) {
        Spacer(Modifier.height(Spacing.sectionGap))
        SectionHeader(title = stringResource(R.string.task_detail_description))
        Spacer(Modifier.height(Spacing.blockGap))
        Text(
            text = task.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    Spacer(Modifier.height(Spacing.sectionGap))
    SectionHeader(title = stringResource(R.string.task_detail_proof))
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = stringResource(
            if (task.status == TaskStatus.COMPLETED) R.string.task_detail_proof_done
            else R.string.task_detail_proof_hint
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.blockGap))
    PhotoAttachmentRow(
        photos = state.photos.map { PhotoAttachment(it.photoId.toString(), it.uri) },
        onAddPhoto = onAddPhoto,
        onRemovePhoto = { onRemovePhoto(it.key.toInt()) },
        maxPhotos = TaskDetailViewModel.MAX_PHOTOS,
        enabled = state.canAddPhotos
    )
}

/** Pending → In progress → Completed, with the current step filled in. */
@Composable
private fun StatusSteps(status: TaskStatus) {
    val steps = listOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED)
    val reached = when (status) {
        TaskStatus.PENDING, TaskStatus.BLOCKED -> 0
        TaskStatus.IN_PROGRESS -> 1
        TaskStatus.COMPLETED -> 2
    }
    val description = stringResource(R.string.task_steps_description, stringResource(status.labelRes))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, step ->
            // Finished steps are green, the current one navy, the rest grey.
            val blocked = status == TaskStatus.BLOCKED
            val done = !blocked && (index < reached || status == TaskStatus.COMPLETED)
            val current = !blocked && index == reached && status != TaskStatus.COMPLETED
            val c = when {
                done -> Tone.Success.colors()
                current -> Tone.Info.colors()
                else -> Tone.Neutral.colors()
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepLine(visible = index > 0, done = !blocked && index <= reached)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (done || current) c.figure else c.container),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
                        } else {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (current) MaterialTheme.colorScheme.surface else c.ink
                            )
                        }
                    }
                    StepLine(visible = index < steps.lastIndex, done = !blocked && index < reached)
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(step.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index == reached) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RowScope.StepLine(visible: Boolean, done: Boolean) {
    HorizontalDivider(
        modifier = Modifier.weight(1f),
        thickness = 3.dp,
        color = when {
            !visible -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            done -> Tone.Success.colors().figure
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    )
}

@Composable
private fun ActionBar(state: TaskDetailUiState, onAdvanceStatus: () -> Unit) {
    if (state.task == null || !state.canUpdate) return
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
        Box(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screenH, vertical = Spacing.md)
        ) {
            AdvanceButton(state, onAdvanceStatus)
        }
    }
}

/** The one big button that moves the task to its next status. */
@Composable
private fun AdvanceButton(state: TaskDetailUiState, onAdvanceStatus: () -> Unit) {
    val task = state.task ?: return
    if (!state.canUpdate) return
    when (state.nextStatus) {
        TaskStatus.IN_PROGRESS -> PrimaryButton(
            text = stringResource(R.string.task_action_start),
            icon = Icons.Filled.PlayArrow,
            loading = state.isUpdating,
            onClick = onAdvanceStatus
        )
        TaskStatus.COMPLETED -> PrimaryButton(
            text = stringResource(R.string.task_action_complete),
            icon = Icons.Filled.TaskAlt,
            loading = state.isUpdating,
            onClick = onAdvanceStatus
        )
        else -> if (task.status == TaskStatus.COMPLETED) {
            StatusChip(
                text = stringResource(R.string.task_action_done),
                tone = Tone.Success,
                icon = Icons.Filled.CheckCircle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Height of the bottom action bar: a 56dp button plus its padding. */
private val ACTION_BAR_HEIGHT = 80.dp

private fun previewState(status: TaskStatus): TaskDetailUiState {
    val today = LocalDate.now()
    return TaskDetailUiState(
        isLoading = false,
        task = Task(
            101, 1, "Pour concrete foundation — Unit 4",
            "Prepare formwork and pour the concrete foundation for Unit 4. Confirm rebar spacing with the foreman before the pour.",
            today, TaskPriority.HIGH, status
        ),
        project = Project(1, 2, "Riverside Estate Phase 2", "Centurion, Gauteng", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0),
        assignees = listOf(
            User(5, UserRole.WORKER, "Sipho", "Dlamini", "", "", "Site Operative", ""),
            User(10, UserRole.WORKER, "Ayanda", "Zulu", "", "", "Steel Fixer", "")
        ),
        canUpdate = true,
        today = today
    )
}

@DevicePreviews
@Composable
private fun TaskDetailPreview() {
    PreviewSurface {
        TaskDetailScreen(previewState(TaskStatus.IN_PROGRESS), false, {}, {}, {}, {}, {}, {}, {})
    }
}

@DevicePreviews
@Composable
private fun TaskDetailCompletedPreview() {
    PreviewSurface {
        TaskDetailScreen(previewState(TaskStatus.COMPLETED), false, {}, {}, {}, {}, {}, {}, {})
    }
}
