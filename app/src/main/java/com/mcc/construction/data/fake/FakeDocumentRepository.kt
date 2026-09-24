package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.DocumentCategory
import com.mcc.construction.data.repository.DocumentRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeDocumentRepository(
    seed: FakeSeedData,
    private val clock: Clock
) : DocumentRepository {

    private val documents = MutableStateFlow(seed.documents)
    private val nextId = AtomicInteger(seed.documents.maxOf { it.documentId } + 1)

    override fun observeDocuments(): Flow<List<Document>> =
        documents.map { list -> list.sortedWith(NEWEST_FIRST) }

    override fun observeDocumentsForProject(projectId: Int): Flow<List<Document>> =
        observeDocuments().map { list -> list.filter { it.projectId == projectId } }

    override fun observeDocument(documentId: Int): Flow<Document?> =
        documents.map { list -> list.firstOrNull { it.documentId == documentId } }

    override suspend fun uploadDocument(
        projectId: Int,
        fileName: String,
        category: DocumentCategory
    ): Document {
        val previous = documents.value
            .filter { it.projectId == projectId && it.fileName.equals(fileName, ignoreCase = true) }
            .maxOfOrNull { it.version } ?: 0
        val document = Document(
            documentId = nextId.getAndIncrement(),
            projectId = projectId,
            fileName = fileName,
            version = previous + 1,
            uploadDate = LocalDateTime.now(clock),
            category = category
        )
        documents.update { it + document }
        return document
    }

    private companion object {
        val NEWEST_FIRST: Comparator<Document> =
            compareByDescending<Document> { it.uploadDate }.thenByDescending { it.documentId }
    }
}
