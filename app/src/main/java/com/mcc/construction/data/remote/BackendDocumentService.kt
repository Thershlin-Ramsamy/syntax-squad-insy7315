package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.DocumentCategory
import com.mcc.construction.data.remote.dto.DocumentRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BackendDocumentService(
    private val client: SupabaseClient,
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport,
    private val storage: BackendStorageService
) {

    fun observeDocuments(): Flow<List<Document>> {
        return reads.observeRows<DocumentRow>(
            table = "documents",
            primaryKey = "document_id"
        ).map { rows ->
            rows.map { row ->
                mapDocument(row)
            }.sortedWith(
                compareByDescending<Document> {
                    it.uploadDate
                }.thenByDescending {
                    it.documentId
                }
            )
        }
    }

    fun observeDocumentsForProject(
        projectId: Int
    ): Flow<List<Document>> {
        return observeDocuments().map { documents ->
            documents.filter {
                it.projectId == projectId
            }
        }
    }

    fun observeDocument(
        documentId: Int
    ): Flow<Document?> {
        return observeDocuments().map { documents ->
            documents.firstOrNull {
                it.documentId == documentId
            }
        }
    }

    suspend fun upload(
        projectId: Int,
        contentUri: String,
        category: DocumentCategory
    ): Document {
        require(projectId > 0)

        val actorId = writes.currentApplicationUserId()

        val upload = storage.uploadDocument(
            projectId = projectId,
            uri = contentUri
        )

        writes.requireCurrentUser(actorId)

        val row = writes.executeAs<DocumentRow>(
            action = BackendAction.DOCUMENT_FINALIZE,
            payload = buildJsonObject {
                put("upload_id", upload.uploadId)
                put("category", category.name)
            }
        )

        return mapDocument(row)
    }

    suspend fun downloadUrl(
        documentId: Int
    ): String {
        require(documentId > 0)

        val actorId = writes.currentApplicationUserId()

        val row = client.from("documents").select {
            filter {
                eq("document_id", documentId)
            }
        }.decodeSingle<DocumentRow>()

        writes.requireCurrentUser(actorId)

        return storage.signedUrl(
            bucketId = SupabaseConfig.DOCUMENT_BUCKET,
            path = row.storagePath
        )
    }

    private fun mapDocument(
        row: DocumentRow
    ): Document {
        return Document(
            documentId = row.documentId,
            projectId = row.projectId,
            fileName = row.fileName,
            version = row.version,
            uploadDate = BackendTime.timestamp(row.uploadDate),
            category = DocumentCategory.valueOf(row.category)
        )
    }
}