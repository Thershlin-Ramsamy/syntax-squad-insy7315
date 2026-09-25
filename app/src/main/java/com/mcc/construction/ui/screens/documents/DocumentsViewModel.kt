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
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A document with its project's name. */
data class DocumentItem(val document: Document, val projectName: String)

/** The upload form, open while [DocumentsUiState.upload] is not null. */
data class UploadForm(
    val projectId: Int? = null,
    val fileName: String = "",
    val category: DocumentCategory = DocumentCategory.DRAWING,
    /** Set while the file is "uploading", from 0 to 1. */
    val progress: Float? = null,
    val nameMissing: Boolean = false
) {
    val isUploading: Boolean get() = progress != null
}

data class DocumentsUiState(
    /** Null until the first load. Newest first. */
    val documents: List<DocumentItem>? = null,
    val projects: List<Project> = emptyList(),
    val projectFilter: Int? = null,
    val categoryFilter: DocumentCategory? = null,
    val canUpload: Boolean = false,
    /** The document whose details and versions are open. */
    val openDocumentId: Int? = null,
    val upload: UploadForm? = null,
    val message: UiMessage? = null
) {
    private val inProject: List<DocumentItem>
        get() = documents.orEmpty().filter { projectFilter == null || it.document.projectId == projectFilter }

    val visible: List<DocumentItem>
        get() = inProject.filter { categoryFilter == null || it.document.category == categoryFilter }

    fun countFor(category: DocumentCategory?): Int = inProject.count { category == null || it.document.category == category }

    val openDocument: DocumentItem? get() = documents?.firstOrNull { it.document.documentId == openDocumentId }

    /** Every version of the open document, newest first. */
    val openVersions: List<Document>
        get() {
            val open = openDocument?.document ?: return emptyList()
            return documents.orEmpty().map { it.document }
                .filter { it.projectId == open.projectId && it.fileName.equals(open.fileName, ignoreCase = true) }
                .sortedByDescending { it.version }
        }
}

/**
 * Drawings, permits, contracts and other files by project, with a simulated
 * upload. There is no file server yet, so the upload records the document and
 * its new version number without sending a file anywhere.
 */
class DocumentsViewModel(
    authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
    projectRepository: ProjectRepository,
    private val uploadDuration: Duration = UPLOAD_DURATION
) : ViewModel() {

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<DocumentsUiState> = combine(
        documentRepository.observeDocuments(),
        projectRepository.observeProjects(),
        authRepository.session,
        local
    ) { documents, projects, session, l ->
        val names = projects.associate { it.projectId to it.projectName }
        DocumentsUiState(
            documents = documents.map { DocumentItem(it, names[it.projectId].orEmpty()) },
            projects = projects,
            projectFilter = l.projectFilter,
            categoryFilter = l.categoryFilter,
            canUpload = session != null && session.role != UserRole.WORKER,
            openDocumentId = l.openDocumentId,
            upload = l.upload,
            message = l.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DocumentsUiState())

    fun onProjectFilterChange(projectId: Int?) = local.update { it.copy(projectFilter = projectId) }

    fun onCategoryFilterChange(category: DocumentCategory?) = local.update { it.copy(categoryFilter = category) }

    fun onOpenDocument(documentId: Int) = local.update { it.copy(openDocumentId = documentId) }

    fun onCloseDocument() = local.update { it.copy(openDocumentId = null) }

    /** Opens the upload form, on the filtered project if there is one. */
    fun onStartUpload() = local.update {
        it.copy(upload = UploadForm(projectId = it.projectFilter ?: uiState.value.projects.firstOrNull()?.projectId))
    }

    /** Opens the upload form for a new version of the open document. */
    fun onUploadNewVersion() {
        val open = uiState.value.openDocument?.document ?: return
        local.update {
            it.copy(
                openDocumentId = null,
                upload = UploadForm(projectId = open.projectId, fileName = open.fileName, category = open.category)
            )
        }
    }

    fun onUploadProjectChange(projectId: Int) = editUpload { it.copy(projectId = projectId) }

    fun onUploadNameChange(name: String) = editUpload { it.copy(fileName = name, nameMissing = false) }

    fun onUploadCategoryChange(category: DocumentCategory) = editUpload { it.copy(category = category) }

    fun onCancelUpload() = local.update { if (it.upload?.isUploading == true) it else it.copy(upload = null) }

    fun onConfirmUpload() {
        val form = local.value.upload ?: return
        if (form.isUploading) return
        val name = form.fileName.trim()
        val projectId = form.projectId
        if (name.isEmpty() || projectId == null) {
            editUpload { it.copy(nameMissing = true) }
            return
        }
        val fileName = if ('.' in name) name else "$name.pdf"
        editUpload { it.copy(progress = 0f) }
        viewModelScope.launch {
            // Stand-in for the transfer, so the progress bar has something to show.
            repeat(UPLOAD_STEPS) { step ->
                delay(uploadDuration / UPLOAD_STEPS)
                editUpload { it.copy(progress = (step + 1f) / UPLOAD_STEPS) }
            }
            val document = documentRepository.uploadDocument(projectId, fileName, form.category)
            local.update {
                it.copy(
                    upload = null,
                    message = UiMessage(R.string.documents_uploaded, listOf(document.fileName, document.version))
                )
            }
        }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    private fun editUpload(change: (UploadForm) -> UploadForm) {
        local.update { l -> l.upload?.let { l.copy(upload = change(it)) } ?: l }
    }

    private data class Local(
        val projectFilter: Int? = null,
        val categoryFilter: DocumentCategory? = null,
        val openDocumentId: Int? = null,
        val upload: UploadForm? = null,
        val message: UiMessage? = null
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val UPLOAD_STEPS = 10
        private val UPLOAD_DURATION = 1_500.milliseconds

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                DocumentsViewModel(c.authRepository, c.documentRepository, c.projectRepository)
            }
        }
    }
}
