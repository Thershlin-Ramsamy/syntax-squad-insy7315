package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.DocumentCategory
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {

    /** Every document, most recently uploaded first. */
    fun observeDocuments(): Flow<List<Document>>

    fun observeDocumentsForProject(projectId: Int): Flow<List<Document>>

    fun observeDocument(documentId: Int): Flow<Document?>

    /**
     * Mirrors the class diagram's `Document.upload()`. Uploading a file with
     * the same name to the same project adds a new version rather than a copy.
     */
    suspend fun uploadDocument(projectId: Int, fileName: String, category: DocumentCategory): Document
}
