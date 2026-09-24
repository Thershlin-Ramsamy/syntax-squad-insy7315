package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Document
import com.mcc.construction.data.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDocumentRepository(seed: FakeSeedData) : DocumentRepository {

    private val documents = MutableStateFlow(seed.documents.sortedByDescending { it.uploadDate })

    override fun observeDocuments(): Flow<List<Document>> = documents

    override fun observeDocumentsForProject(projectId: Int): Flow<List<Document>> =
        documents.map { list -> list.filter { it.projectId == projectId } }

    override fun observeDocument(documentId: Int): Flow<Document?> =
        documents.map { list -> list.firstOrNull { it.documentId == documentId } }
}
