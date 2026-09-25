package com.mcc.construction.ui.screens.documents

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.DocumentCategory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.ChoiceChip
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@get:StringRes
val DocumentCategory.labelRes: Int
    get() = when (this) {
        DocumentCategory.DRAWING -> R.string.document_category_drawing
        DocumentCategory.PERMIT -> R.string.document_category_permit
        DocumentCategory.CONTRACT -> R.string.document_category_contract
        DocumentCategory.SAFETY -> R.string.document_category_safety
        DocumentCategory.REPORT -> R.string.document_category_report
        DocumentCategory.OTHER -> R.string.document_category_other
    }

val DocumentCategory.icon: ImageVector
    get() = when (this) {
        DocumentCategory.DRAWING -> Icons.Filled.Architecture
        DocumentCategory.PERMIT -> Icons.Filled.VerifiedUser
        DocumentCategory.CONTRACT -> Icons.Filled.Gavel
        DocumentCategory.SAFETY -> Icons.Filled.HealthAndSafety
        DocumentCategory.REPORT -> Icons.Filled.Assessment
        DocumentCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

/** Callbacks for the Documents screen. */
data class DocumentsActions(
    val onProjectFilterChange: (Int?) -> Unit = {},
    val onCategoryFilterChange: (DocumentCategory?) -> Unit = {},
    val onOpenDocument: (Int) -> Unit = {},
    val onCloseDocument: () -> Unit = {},
    val onStartUpload: () -> Unit = {},
    val onUploadNewVersion: () -> Unit = {},
    val onUploadProjectChange: (Int) -> Unit = {},
    val onUploadNameChange: (String) -> Unit = {},
    val onUploadCategoryChange: (DocumentCategory) -> Unit = {},
    val onCancelUpload: () -> Unit = {},
    val onConfirmUpload: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

@Composable
fun DocumentsRoute(viewModel: DocumentsViewModel = viewModel(factory = DocumentsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DocumentsScreen(
        state = state,
        actions = DocumentsActions(
            onProjectFilterChange = viewModel::onProjectFilterChange,
            onCategoryFilterChange = viewModel::onCategoryFilterChange,
            onOpenDocument = viewModel::onOpenDocument,
            onCloseDocument = viewModel::onCloseDocument,
            onStartUpload = viewModel::onStartUpload,
            onUploadNewVersion = viewModel::onUploadNewVersion,
            onUploadProjectChange = viewModel::onUploadProjectChange,
            onUploadNameChange = viewModel::onUploadNameChange,
            onUploadCategoryChange = viewModel::onUploadCategoryChange,
            onCancelUpload = viewModel::onCancelUpload,
            onConfirmUpload = viewModel::onConfirmUpload,
            onMessageShown = viewModel::onMessageShown
        )
    )
}

/**
 * Drawings, permits, contracts and other files, filtered by project and kind,
 * each with its version and upload date. Managers can upload; everyone can
 * open a document to see its version history. On a wide window the list is
 * two columns.
 */
@Composable
fun DocumentsScreen(
    state: DocumentsUiState,
    actions: DocumentsActions,
    modifier: Modifier = Modifier
) {
    if (state.documents == null) {
        LoadingState(modifier)
        return
    }
    val wide = LocalWindowLayout.current.isExpanded
    ActionFeedbackHost(state.message, actions.onMessageShown, modifier) {
        ScreenScaffold(scrollable = false, maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth) {
            Filters(state, actions, wide)
            Spacer(Modifier.height(Spacing.blockGap))
            if (state.visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.FolderOpen,
                    title = stringResource(R.string.documents_empty_title),
                    message = stringResource(R.string.documents_empty_message),
                    actionLabel = if (state.canUpload) stringResource(R.string.documents_upload) else null,
                    onAction = if (state.canUpload) actions.onStartUpload else null
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (wide) 2 else 1),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = Spacing.xxxl),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (!wide && state.canUpload) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PrimaryButton(
                                text = stringResource(R.string.documents_upload),
                                onClick = actions.onStartUpload,
                                icon = Icons.Filled.UploadFile,
                                modifier = Modifier.padding(bottom = Spacing.sm)
                            )
                        }
                    }
                    items(state.visible, key = { it.document.documentId }) { item ->
                        DocumentRow(item) { actions.onOpenDocument(item.document.documentId) }
                    }
                }
            }
        }
    }

    state.openDocument?.let { DocumentDialog(it, state, actions) }
    state.upload?.let { UploadDialog(it, state.projects, actions) }
}

@Composable
private fun Filters(state: DocumentsUiState, actions: DocumentsActions, wide: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            ChoiceChipGroup(
                options = listOf<Int?>(null) + state.projects.filter { p -> state.documents.orEmpty().any { it.document.projectId == p.projectId } }.map { it.projectId },
                selected = state.projectFilter,
                onSelect = actions.onProjectFilterChange,
                label = { id ->
                    if (id == null) stringResource(R.string.filter_all_projects) else state.projects.first { it.projectId == id }.projectName
                }
            )
            Spacer(Modifier.height(Spacing.sm))
            ChoiceChipGroup(
                options = listOf(null) + DocumentCategory.entries.filter { state.countFor(it) > 0 || it == state.categoryFilter },
                selected = state.categoryFilter,
                onSelect = actions.onCategoryFilterChange,
                label = { category ->
                    val name = if (category == null) stringResource(R.string.filter_all) else stringResource(category.labelRes)
                    stringResource(R.string.filter_with_count, name, state.countFor(category))
                },
                icon = { category -> category?.icon ?: Icons.Filled.FolderOpen }
            )
        }
        if (wide && state.canUpload) {
            Spacer(Modifier.width(Spacing.lg))
            PrimaryButton(
                text = stringResource(R.string.documents_upload),
                onClick = actions.onStartUpload,
                icon = Icons.Filled.UploadFile,
                modifier = Modifier.widthIn(max = 240.dp)
            )
        }
    }
}

@Composable
private fun DocumentRow(item: DocumentItem, onClick: () -> Unit) {
    val doc = item.document
    ListRow(
        title = doc.fileName,
        subtitle = stringResource(R.string.document_version_date, stringResource(doc.category.labelRes), doc.version, doc.uploadDate.formatDate()),
        meta = item.projectName,
        onClick = onClick,
        leading = { LeadingIcon(doc.category.icon, Tone.Info) },
        trailing = { if (doc.extension.isNotEmpty()) StatusChip(doc.extension, Tone.Neutral) }
    )
}

@Composable
private fun DocumentDialog(item: DocumentItem, state: DocumentsUiState, actions: DocumentsActions) {
    val doc = item.document
    AlertDialog(
        onDismissRequest = actions.onCloseDocument,
        icon = { Icon(doc.category.icon, contentDescription = null) },
        title = { Text(doc.fileName) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                KeyValueRow(stringResource(R.string.documents_project), item.projectName)
                Spacer(Modifier.height(Spacing.sm))
                KeyValueRow(stringResource(R.string.documents_category), stringResource(doc.category.labelRes))
                Spacer(Modifier.height(Spacing.sm))
                KeyValueRow(stringResource(R.string.documents_latest_version), stringResource(R.string.document_version, state.openVersions.firstOrNull()?.version ?: doc.version))
                Spacer(Modifier.height(Spacing.lg))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.documents_history), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(Spacing.sm))
                state.openVersions.forEachIndexed { index, version ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = Spacing.xs), color = MaterialTheme.colorScheme.outlineVariant)
                    KeyValueRow(stringResource(R.string.document_version, version.version), version.uploadDate.formatDate())
                }
            }
        },
        confirmButton = {
            if (state.canUpload) {
                TextButton(onClick = actions.onUploadNewVersion) { Text(stringResource(R.string.documents_new_version)) }
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCloseDocument) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadDialog(form: UploadForm, projects: List<Project>, actions: DocumentsActions) {
    AlertDialog(
        onDismissRequest = actions.onCancelUpload,
        icon = { Icon(Icons.Filled.UploadFile, contentDescription = null) },
        title = { Text(stringResource(R.string.documents_upload_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppTextField(
                    value = form.fileName,
                    onValueChange = actions.onUploadNameChange,
                    label = stringResource(R.string.documents_file_name),
                    placeholder = stringResource(R.string.documents_file_name_hint),
                    isError = form.nameMissing,
                    supportingText = if (form.nameMissing) stringResource(R.string.documents_file_name_required) else null,
                    enabled = !form.isUploading,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.md))
                Text(stringResource(R.string.documents_project), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.xs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    projects.forEach { project ->
                        ChoiceChip(
                            label = project.projectName,
                            selected = form.projectId == project.projectId,
                            onClick = { actions.onUploadProjectChange(project.projectId) },
                            enabled = !form.isUploading
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                Text(stringResource(R.string.documents_category), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.xs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DocumentCategory.entries.forEach { category ->
                        ChoiceChip(
                            label = stringResource(category.labelRes),
                            selected = form.category == category,
                            onClick = { actions.onUploadCategoryChange(category) },
                            icon = category.icon,
                            enabled = !form.isUploading
                        )
                    }
                }
                form.progress?.let { progress ->
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        stringResource(R.string.documents_uploading, (progress * 100).toInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirmUpload, enabled = !form.isUploading) {
                Text(stringResource(R.string.documents_upload_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCancelUpload, enabled = !form.isUploading) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private val WIDE_CONTENT = 1200.dp

private fun previewState(upload: Boolean = false): DocumentsUiState {
    val now = LocalDateTime.now()
    val today = LocalDate.now()
    val riverside = Project(1, 2, "Riverside Estate Phase 2", "Centurion", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0)
    val harbour = riverside.copy(projectId = 2, projectName = "Harbour View Offices")
    return DocumentsUiState(
        documents = listOf(
            DocumentItem(Document(3, 1, "Riverside-Health-and-Safety-File.pdf", 5, now.minusDays(4), DocumentCategory.SAFETY), riverside.projectName),
            DocumentItem(Document(10, 2, "HarbourView-Level-3-Slab-Drawings-RevB.dwg", 2, now.minusDays(9), DocumentCategory.DRAWING), harbour.projectName),
            DocumentItem(Document(1, 1, "Riverside-Structural-Drawings-RevC.pdf", 3, now.minusDays(13), DocumentCategory.DRAWING), riverside.projectName),
            DocumentItem(Document(9, 2, "HarbourView-Building-Plan-Approval.pdf", 1, now.minusDays(210), DocumentCategory.PERMIT), harbour.projectName),
            DocumentItem(Document(8, 1, "Riverside-Main-Contract-Signed.pdf", 1, now.minusDays(262), DocumentCategory.CONTRACT), riverside.projectName)
        ),
        projects = listOf(riverside, harbour),
        canUpload = true,
        upload = if (upload) UploadForm(projectId = 1, fileName = "Site-Plan-RevA.pdf", progress = 0.6f) else null
    )
}

@DevicePreviews
@Composable
private fun DocumentsPreview() {
    PreviewSurface { DocumentsScreen(previewState(), DocumentsActions()) }
}

@DevicePreviews
@Composable
private fun DocumentsUploadPreview() {
    PreviewSurface { DocumentsScreen(previewState(upload = true), DocumentsActions()) }
}
