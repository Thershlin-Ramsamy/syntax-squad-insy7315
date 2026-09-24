package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {

    /** Every document, most recently uploaded first. */
    fun observeDocuments(): Flow<List<Document>>

    fun observeDocumentsForProject(projectId: Int): Flow<List<Document>>

    fun observeDocument(documentId: Int): Flow<Document?>
}
