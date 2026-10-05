package com.mcc.construction.ui.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.DocumentCategory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.DocumentRepository
import com.mcc.construction.data.repository.FileDocumentRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentItem(
    val document: Document,
    val projectName: String
)

data class UploadForm(
    val projectId: Int? = null,
    val fileName: String = "",
    val category: DocumentCategory = DocumentCategory.DRAWING,
    val progress: Float? = null,
    val nameMissing: Boolean = false,
    val contentUri: String? = null
) {
    val isUploading: Boolean
        get() = progress != null
}

data class DocumentsUiState(
    val documents: List<DocumentItem>? = null,
    val projects: List<Project> = emptyList(),
    val projectFilter: Int? = null,
    val categoryFilter: DocumentCategory? = null,
    val canUpload: Boolean = false,
    val openDocumentId: Int? = null,
    val upload: UploadForm? = null,
    val message: UiMessage? = null,
    val requiresSelectedFile: Boolean = false,
    val downloadUrl: String? = null,
    val isDownloading: Boolean = false
) {
    private val inProject: List<DocumentItem>
        get() = documents.orEmpty().filter {
            projectFilter == null ||
                    it.document.projectId == projectFilter
        }

    val visible: List<DocumentItem>
        get() = inProject.filter {
            categoryFilter == null ||
                    it.document.category == categoryFilter
        }

    fun countFor(
        category: DocumentCategory?
    ): Int {
        return inProject.count {
            category == null ||
                    it.document.category == category
        }
    }

    val openDocument: DocumentItem?
        get() = documents?.firstOrNull {
            it.document.documentId == openDocumentId
        }

    val openVersions: List<Document>
        get() {
            val open = openDocument?.document
                ?: return emptyList()

            return documents.orEmpty()
                .map { it.document }
                .filter {
                    it.projectId == open.projectId &&
                            it.fileName.equals(
                                open.fileName,
                                ignoreCase = true
                            )
                }
                .sortedByDescending {
                    it.version
                }
        }
}

class DocumentsViewModel(
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
    projectRepository: ProjectRepository,
    @Suppress("UNUSED_PARAMETER")
    private val uploadDuration: Duration = Duration.ZERO
) : ViewModel() {

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<DocumentsUiState> = combine(
        documentRepository.observeDocuments(),
        projectRepository.observeProjects(),
        authRepository.session,
        local
    ) { documents, projects, session, l ->
        val names = projects.associate {
            it.projectId to it.projectName
        }

        DocumentsUiState(
            documents = documents.map {
                DocumentItem(
                    it,
                    names[it.projectId].orEmpty()
                )
            },
            projects = projects,
            projectFilter = l.projectFilter,
            categoryFilter = l.categoryFilter,
            canUpload = session != null &&
                    session.role != UserRole.WORKER,
            openDocumentId = l.openDocumentId,
            upload = l.upload,
            message = l.message,
            requiresSelectedFile =
                documentRepository is FileDocumentRepository,
            downloadUrl = l.downloadUrl,
            isDownloading = l.isDownloading
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        DocumentsUiState()
    )

    fun onProjectFilterChange(projectId: Int?) {
        local.update {
            it.copy(projectFilter = projectId)
        }
    }

    fun onCategoryFilterChange(
        category: DocumentCategory?
    ) {
        local.update {
            it.copy(categoryFilter = category)
        }
    }

    fun onOpenDocument(documentId: Int) {
        local.update {
            it.copy(openDocumentId = documentId)
        }
    }

    fun onCloseDocument() {
        local.update {
            it.copy(openDocumentId = null)
        }
    }

    fun onStartUpload() {
        if (!canUploadNow()) return

        local.update {
            it.copy(
                upload = UploadForm(
                    projectId = it.projectFilter
                        ?: uiState.value.projects
                            .firstOrNull()?.projectId
                )
            )
        }
    }

    fun onUploadNewVersion() {
        if (!canUploadNow()) return

        val open = uiState.value.openDocument
            ?.document ?: return

        local.update {
            it.copy(
                openDocumentId = null,
                upload = UploadForm(
                    projectId = open.projectId,
                    fileName = open.fileName,
                    category = open.category
                )
            )
        }
    }

    fun onUploadProjectChange(projectId: Int) {
        editUpload {
            it.copy(projectId = projectId)
        }
    }

    fun onUploadNameChange(name: String) {
        editUpload {
            it.copy(
                fileName = name,
                nameMissing = false
            )
        }
    }

    fun onUploadCategoryChange(
        category: DocumentCategory
    ) {
        editUpload {
            it.copy(category = category)
        }
    }

    fun onFileSelected(
        contentUri: String,
        displayName: String
    ) {
        editUpload {
            it.copy(
                contentUri = contentUri,
                fileName = displayName,
                nameMissing = false
            )
        }
    }

    fun onFileSelectionFailed() {
        showError(R.string.backend_operation_failed)
    }

    fun onCancelUpload() {
        local.update {
            if (it.upload?.isUploading == true) {
                it
            } else {
                it.copy(upload = null)
            }
        }
    }

    fun onConfirmUpload() {
        if (!canUploadNow()) return

        val form = local.value.upload ?: return

        if (form.isUploading) return

        val projectId = form.projectId
        val name = form.fileName.trim()

        if (projectId == null || name.isEmpty()) {
            editUpload {
                it.copy(nameMissing = true)
            }
            return
        }

        val files = documentRepository as? FileDocumentRepository

        if (
            files != null &&
            form.contentUri.isNullOrBlank()
        ) {
            showError(R.string.backend_file_required)
            return
        }

        editUpload {
            it.copy(progress = 0f)
        }

        viewModelScope.launch {
            try {
                val document = if (files != null) {
                    files.uploadSelectedDocument(
                        projectId = projectId,
                        contentUri = requireNotNull(form.contentUri),
                        category = form.category
                    )
                } else {
                    val fileName = if ('.' in name) {
                        name
                    } else {
                        "$name.pdf"
                    }

                    documentRepository.uploadDocument(
                        projectId,
                        fileName,
                        form.category
                    )
                }

                local.update {
                    it.copy(
                        upload = null,
                        message = UiMessage(
                            R.string.documents_uploaded,
                            listOf(
                                document.fileName,
                                document.version
                            )
                        )
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                showError(R.string.backend_operation_failed)
            } finally {
                editUpload {
                    it.copy(progress = null)
                }
            }
        }
    }

    fun onDownload(documentId: Int) {
        if (local.value.isDownloading) return

        val files = documentRepository as? FileDocumentRepository

        if (files == null) {
            showError(R.string.backend_download_unavailable)
            return
        }

        local.update {
            it.copy(isDownloading = true)
        }

        viewModelScope.launch {
            try {
                val url = files.getDownloadUrl(documentId)

                local.update {
                    it.copy(downloadUrl = url)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                showError(R.string.backend_operation_failed)
            } finally {
                local.update {
                    it.copy(isDownloading = false)
                }
            }
        }
    }

    fun onDownloadHandled() {
        local.update {
            it.copy(downloadUrl = null)
        }
    }

    fun onDownloadOpenFailed() {
        local.update {
            it.copy(
                downloadUrl = null,
                message = UiMessage(
                    R.string.backend_no_file_viewer,
                    isError = true
                )
            )
        }
    }

    fun onMessageShown() {
        local.update {
            it.copy(message = null)
        }
    }

    private fun canUploadNow(): Boolean {
        val session = authRepository.session.value
            ?: return false

        return session.role != UserRole.WORKER
    }

    private fun showError(resource: Int) {
        local.update {
            it.copy(
                message = UiMessage(
                    resource,
                    isError = true
                )
            )
        }
    }

    private fun editUpload(
        change: (UploadForm) -> UploadForm
    ) {
        local.update { l ->
            l.upload?.let {
                l.copy(upload = change(it))
            } ?: l
        }
    }

    private data class Local(
        val projectFilter: Int? = null,
        val categoryFilter: DocumentCategory? = null,
        val openDocumentId: Int? = null,
        val upload: UploadForm? = null,
        val message: UiMessage? = null,
        val downloadUrl: String? = null,
        val isDownloading: Boolean = false
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()

                DocumentsViewModel(
                    c.authRepository,
                    c.documentRepository,
                    c.projectRepository
                )
            }
        }
    }
}