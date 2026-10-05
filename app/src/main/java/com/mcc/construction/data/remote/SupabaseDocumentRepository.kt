package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.DocumentCategory
import com.mcc.construction.data.repository.FileDocumentRepository
import kotlinx.coroutines.flow.Flow

class SupabaseDocumentRepository(
    private val service: BackendDocumentService
) : FileDocumentRepository {

    override fun observeDocuments(): Flow<List<Document>> {
        return service.observeDocuments()
    }

    override fun observeDocumentsForProject(
        projectId: Int
    ): Flow<List<Document>> {
        return service.observeDocumentsForProject(projectId)
    }

    override fun observeDocument(
        documentId: Int
    ): Flow<Document?> {
        return service.observeDocument(documentId)
    }

    override suspend fun uploadSelectedDocument(
        projectId: Int,
        contentUri: String,
        category: DocumentCategory
    ): Document {
        return service.upload(
            projectId = projectId,
            contentUri = contentUri,
            category = category
        )
    }

    override suspend fun getDownloadUrl(
        documentId: Int
    ): String {
        return service.downloadUrl(documentId)
    }

    override suspend fun uploadDocument(
        projectId: Int,
        fileName: String,
        category: DocumentCategory
    ): Document {
        throw IllegalArgumentException(
            "A filename does not provide file contents. " +
                    "Select a document and call uploadSelectedDocument " +
                    "with its Android content URI."
        )
    }
}