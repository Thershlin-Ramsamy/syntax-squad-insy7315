package com.mcc.construction.ui.screens.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.*
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

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
        DocumentCategory.OTHER ->
            Icons.AutoMirrored.Filled.InsertDriveFile
    }

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
    val onMessageShown: () -> Unit = {},
    val onChooseFile: () -> Unit = {},
    val onDownload: (Int) -> Unit = {}
)

@Composable
fun DocumentsRoute(
    viewModel: DocumentsViewModel =
        viewModel(factory = DocumentsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                require(uri.scheme == "content")

                val name = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val column = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                    if (
                        column >= 0 &&
                        cursor.moveToFirst()
                    ) {
                        cursor.getString(column)
                    } else {
                        null
                    }
                } ?: uri.lastPathSegment ?: "document"

                viewModel.onFileSelected(
                    uri.toString(),
                    name
                )
            } catch (_: Exception) {
                viewModel.onFileSelectionFailed()
            }
        }
    }

    LaunchedEffect(state.downloadUrl) {
        val url = state.downloadUrl
            ?: return@LaunchedEffect

        try {
            val parsed = Uri.parse(url)

            require(parsed.scheme == "https")

            context.startActivity(
                Intent(Intent.ACTION_VIEW, parsed)
            )

            viewModel.onDownloadHandled()
        } catch (_: ActivityNotFoundException) {
            viewModel.onDownloadOpenFailed()
        } catch (_: Exception) {
            viewModel.onDownloadOpenFailed()
        }
    }

    DocumentsScreen(
        state = state,
        actions = DocumentsActions(
            onProjectFilterChange =
                viewModel::onProjectFilterChange,
            onCategoryFilterChange =
                viewModel::onCategoryFilterChange,
            onOpenDocument = viewModel::onOpenDocument,
            onCloseDocument = viewModel::onCloseDocument,
            onStartUpload = viewModel::onStartUpload,
            onUploadNewVersion = viewModel::onUploadNewVersion,
            onUploadProjectChange =
                viewModel::onUploadProjectChange,
            onUploadNameChange = viewModel::onUploadNameChange,
            onUploadCategoryChange =
                viewModel::onUploadCategoryChange,
            onCancelUpload = viewModel::onCancelUpload,
            onConfirmUpload = viewModel::onConfirmUpload,
            onMessageShown = viewModel::onMessageShown,
            onChooseFile = {
                picker.launch(
                    arrayOf(
                        "application/pdf",
                        "image/jpeg",
                        "image/png",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                )
            },
            onDownload = viewModel::onDownload
        )
    )
}

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

    ActionFeedbackHost(
        state.message,
        actions.onMessageShown,
        modifier
    ) {
        ScreenScaffold(
            scrollable = false,
            maxContentWidth = if (wide) {
                1200.dp
            } else {
                Spacing.contentMaxWidth
            }
        ) {
            Filters(state, actions, wide)

            Spacer(Modifier.height(Spacing.blockGap))

            if (state.visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.FolderOpen,
                    title = stringResource(
                        R.string.documents_empty_title
                    ),
                    message = stringResource(
                        R.string.documents_empty_message
                    ),
                    actionLabel = if (state.canUpload) {
                        stringResource(R.string.documents_upload)
                    } else {
                        null
                    },
                    onAction = if (state.canUpload) {
                        actions.onStartUpload
                    } else {
                        null
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(
                        if (wide) 2 else 1
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        bottom = Spacing.xxxl
                    ),
                    horizontalArrangement =
                        Arrangement.spacedBy(Spacing.itemGap),
                    verticalArrangement =
                        Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (!wide && state.canUpload) {
                        item(
                            span = {
                                GridItemSpan(maxLineSpan)
                            }
                        ) {
                            PrimaryButton(
                                text = stringResource(
                                    R.string.documents_upload
                                ),
                                onClick = actions.onStartUpload,
                                icon = Icons.Filled.UploadFile,
                                modifier = Modifier.padding(
                                    bottom = Spacing.sm
                                )
                            )
                        }
                    }

                    items(
                        state.visible,
                        key = { it.document.documentId }
                    ) { item ->
                        DocumentRow(item) {
                            actions.onOpenDocument(
                                item.document.documentId
                            )
                        }
                    }
                }
            }
        }
    }

    state.openDocument?.let {
        DocumentDialog(it, state, actions)
    }

    state.upload?.let {
        UploadDialog(
            it,
            state.projects,
            state.requiresSelectedFile,
            actions
        )
    }
}

@Composable
private fun Filters(
    state: DocumentsUiState,
    actions: DocumentsActions,
    wide: Boolean
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            ChoiceChipGroup(
                options = listOf<Int?>(null) +
                        state.projects.filter { project ->
                            state.documents.orEmpty().any {
                                it.document.projectId ==
                                        project.projectId
                            }
                        }.map {
                            it.projectId
                        },
                selected = state.projectFilter,
                onSelect = actions.onProjectFilterChange,
                label = { id ->
                    if (id == null) {
                        stringResource(
                            R.string.filter_all_projects
                        )
                    } else {
                        state.projects.first {
                            it.projectId == id
                        }.projectName
                    }
                }
            )

            Spacer(Modifier.height(Spacing.sm))

            ChoiceChipGroup(
                options = listOf(null) +
                        DocumentCategory.entries.filter {
                            state.countFor(it) > 0 ||
                                    it == state.categoryFilter
                        },
                selected = state.categoryFilter,
                onSelect = actions.onCategoryFilterChange,
                label = { category ->
                    val name = if (category == null) {
                        stringResource(R.string.filter_all)
                    } else {
                        stringResource(category.labelRes)
                    }

                    stringResource(
                        R.string.filter_with_count,
                        name,
                        state.countFor(category)
                    )
                },
                icon = {
                    it?.icon ?: Icons.Filled.FolderOpen
                }
            )
        }

        if (wide && state.canUpload) {
            Spacer(Modifier.width(Spacing.lg))

            PrimaryButton(
                text = stringResource(
                    R.string.documents_upload
                ),
                onClick = actions.onStartUpload,
                icon = Icons.Filled.UploadFile,
                modifier = Modifier.widthIn(max = 240.dp)
            )
        }
    }
}

@Composable
private fun DocumentRow(
    item: DocumentItem,
    onClick: () -> Unit
) {
    val document = item.document

    ListRow(
        title = document.fileName,
        subtitle = stringResource(
            R.string.document_version_date,
            stringResource(document.category.labelRes),
            document.version,
            document.uploadDate.formatDate()
        ),
        meta = item.projectName,
        onClick = onClick,
        leading = {
            LeadingIcon(
                document.category.icon,
                Tone.Info
            )
        },
        trailing = {
            if (document.extension.isNotEmpty()) {
                StatusChip(
                    document.extension,
                    Tone.Neutral
                )
            }
        }
    )
}

@Composable
private fun DocumentDialog(
    item: DocumentItem,
    state: DocumentsUiState,
    actions: DocumentsActions
) {
    val document = item.document

    AlertDialog(
        onDismissRequest = actions.onCloseDocument,
        icon = {
            Icon(
                document.category.icon,
                contentDescription = null
            )
        },
        title = {
            Text(document.fileName)
        },
        text = {
            Column(
                Modifier.verticalScroll(
                    rememberScrollState()
                )
            ) {
                KeyValueRow(
                    stringResource(R.string.documents_project),
                    item.projectName
                )

                Spacer(Modifier.height(Spacing.sm))

                KeyValueRow(
                    stringResource(R.string.documents_category),
                    stringResource(document.category.labelRes)
                )

                Spacer(Modifier.height(Spacing.sm))

                KeyValueRow(
                    stringResource(
                        R.string.documents_latest_version
                    ),
                    stringResource(
                        R.string.document_version,
                        state.openVersions.firstOrNull()?.version
                            ?: document.version
                    )
                )

                Spacer(Modifier.height(Spacing.lg))

                Text(
                    stringResource(R.string.documents_history),
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(Spacing.sm))

                state.openVersions.forEachIndexed {
                        index, version ->

                    if (index > 0) {
                        HorizontalDivider(
                            Modifier.padding(
                                vertical = Spacing.xs
                            )
                        )
                    }

                    KeyValueRow(
                        stringResource(
                            R.string.document_version,
                            version.version
                        ),
                        version.uploadDate.formatDate()
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                TextButton(
                    onClick = {
                        actions.onDownload(document.documentId)
                    },
                    enabled = !state.isDownloading
                ) {
                    Text(
                        stringResource(
                            R.string.backend_open_document
                        )
                    )
                }
            }
        },
        confirmButton = {
            if (state.canUpload) {
                TextButton(
                    onClick = actions.onUploadNewVersion
                ) {
                    Text(
                        stringResource(
                            R.string.documents_new_version
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = actions.onCloseDocument
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadDialog(
    form: UploadForm,
    projects: List<Project>,
    requiresSelectedFile: Boolean,
    actions: DocumentsActions
) {
    AlertDialog(
        onDismissRequest = actions.onCancelUpload,
        icon = {
            Icon(
                Icons.Filled.UploadFile,
                contentDescription = null
            )
        },
        title = {
            Text(
                stringResource(
                    R.string.documents_upload_title
                )
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(
                    rememberScrollState()
                )
            ) {
                if (requiresSelectedFile) {
                    TextButton(
                        onClick = actions.onChooseFile,
                        enabled = !form.isUploading
                    ) {
                        Text(
                            stringResource(
                                R.string.backend_choose_file
                            )
                        )
                    }
                }

                AppTextField(
                    value = form.fileName,
                    onValueChange = actions.onUploadNameChange,
                    label = stringResource(
                        R.string.documents_file_name
                    ),
                    placeholder = stringResource(
                        R.string.documents_file_name_hint
                    ),
                    isError = form.nameMissing,
                    supportingText = if (form.nameMissing) {
                        stringResource(
                            R.string.documents_file_name_required
                        )
                    } else {
                        null
                    },
                    enabled = !form.isUploading &&
                            !requiresSelectedFile,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(Spacing.md))

                Text(
                    stringResource(R.string.documents_project),
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement =
                        Arrangement.spacedBy(Spacing.sm)
                ) {
                    projects.forEach { project ->
                        ChoiceChip(
                            label = project.projectName,
                            selected =
                                form.projectId == project.projectId,
                            onClick = {
                                actions.onUploadProjectChange(
                                    project.projectId
                                )
                            },
                            enabled = !form.isUploading
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                Text(
                    stringResource(R.string.documents_category),
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement =
                        Arrangement.spacedBy(Spacing.sm)
                ) {
                    DocumentCategory.entries.forEach {
                            category ->

                        ChoiceChip(
                            label = stringResource(
                                category.labelRes
                            ),
                            selected =
                                form.category == category,
                            onClick = {
                                actions.onUploadCategoryChange(
                                    category
                                )
                            },
                            icon = category.icon,
                            enabled = !form.isUploading
                        )
                    }
                }

                if (form.isUploading) {
                    Spacer(Modifier.height(Spacing.lg))

                    Text(
                        stringResource(
                            R.string.backend_upload_in_progress
                        )
                    )

                    Spacer(Modifier.height(Spacing.xs))

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions.onConfirmUpload,
                enabled = !form.isUploading
            ) {
                Text(
                    stringResource(
                        R.string.documents_upload_confirm
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = actions.onCancelUpload,
                enabled = !form.isUploading
            ) {
                Text(
                    stringResource(R.string.action_cancel)
                )
            }
        }
    )
}

private fun previewDocumentsState(): DocumentsUiState {
    val today = LocalDate.now()
    val now = LocalDateTime.now()
    val projects = listOf(
        Project(1, 2, "Riverside Estate Phase 2", "Centurion, Gauteng", BigDecimal("48200000"), today.minusDays(255), today.plusDays(68), ProjectStatus.ACTIVE, 68.0),
        Project(2, 2, "Harbour View Offices", "Foreshore, Cape Town", BigDecimal("48200000"), today.minusDays(100), today.plusDays(200), ProjectStatus.ACTIVE, 41.5)
    )
    val documents = listOf(
        DocumentItem(
            document = Document(1, 1, "Riverside-Structural-Drawings-RevC.pdf", 3, now.minusDays(13), DocumentCategory.DRAWING),
            projectName = "Riverside Estate Phase 2"
        ),
        DocumentItem(
            document = Document(2, 1, "Building-Permit-Approved.pdf", 1, now.minusDays(30), DocumentCategory.PERMIT),
            projectName = "Riverside Estate Phase 2"
        ),
        DocumentItem(
            document = Document(3, 2, "Harbour-View-Contract.pdf", 2, now.minusDays(45), DocumentCategory.CONTRACT),
            projectName = "Harbour View Offices"
        ),
        DocumentItem(
            document = Document(4, 2, "Safety-Plan-v2.pdf", 2, now.minusDays(5), DocumentCategory.SAFETY),
            projectName = "Harbour View Offices"
        )
    )
    return DocumentsUiState(
        documents = documents,
        projects = projects,
        canUpload = true
    )
}

@DevicePreviews
@Composable
private fun DocumentsScreenPreview() {
    PreviewSurface {
        DocumentsScreen(
            state = previewDocumentsState(),
            actions = DocumentsActions()
        )
    }
}