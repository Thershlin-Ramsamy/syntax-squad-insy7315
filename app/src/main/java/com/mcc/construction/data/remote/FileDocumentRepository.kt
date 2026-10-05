package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.DocumentCategory

interface FileDocumentRepository : DocumentRepository {

    suspend fun uploadSelectedDocument(
        projectId: Int,
        contentUri: String,
        category: DocumentCategory
    ): Document

    suspend fun getDownloadUrl(
        documentId: Int
    ): String
}