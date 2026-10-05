package com.mcc.construction.data.remote

import com.mcc.construction.data.remote.dto.FileUploadRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BackendStorageService(
    private val client: SupabaseClient,
    private val reader: AndroidUploadReader,
    private val writes: BackendRepositorySupport
) {
    private data class CachedUrl(
        val url: String,
        val createdAt: Long
    )

    private val signedUrls = mutableMapOf<String, CachedUrl>()
    private val signedUrlMutex = Mutex()
    private var cachedAccountId: String? = null

    suspend fun uploadPhoto(
        projectId: Int,
        uri: String
    ): FileUploadRow {
        return upload(
            projectId = projectId,
            bucketId = SupabaseConfig.PHOTO_BUCKET,
            uri = uri
        )
    }

    suspend fun uploadDocument(
        projectId: Int,
        uri: String
    ): FileUploadRow {
        return upload(
            projectId = projectId,
            bucketId = SupabaseConfig.DOCUMENT_BUCKET,
            uri = uri
        )
    }

    suspend fun photoUrls(paths: List<String>): List<String> {
        return paths.map { path ->
            signedUrl(
                bucketId = SupabaseConfig.PHOTO_BUCKET,
                path = path
            )
        }
    }

    suspend fun signedUrl(
        bucketId: String,
        path: String
    ): String {
        BackendFileRules.limit(bucketId)

        require(path.isNotBlank()) {
            "A storage path is required."
        }

        val accountId = currentAccountId()

        return signedUrlMutex.withLock {
            checkAccount(accountId)

            if (cachedAccountId != accountId) {
                signedUrls.clear()
                cachedAccountId = accountId
            }

            val cacheKey = "$bucketId:$path"
            val now = System.currentTimeMillis()
            val cached = signedUrls[cacheKey]

            if (
                cached != null &&
                now >= cached.createdAt &&
                now - cached.createdAt < 8 * 60_000L
            ) {
                return@withLock cached.url
            }

            val url = client.storage.from(bucketId).createSignedUrl(
                path = path,
                expiresIn = 10.minutes
            )

            checkAccount(accountId)

            if (signedUrls.size >= 500) {
                signedUrls.clear()
            }

            signedUrls[cacheKey] = CachedUrl(
                url = url,
                createdAt = now
            )

            url
        }
    }

    suspend fun deleteRemovedPhoto(path: String) {
        val accountId = currentAccountId()

        client.storage.from(SupabaseConfig.PHOTO_BUCKET)
            .delete(listOf(path))

        checkAccount(accountId)
    }

    private suspend fun upload(
        projectId: Int,
        bucketId: String,
        uri: String
    ): FileUploadRow {
        require(projectId > 0)

        val accountId = currentAccountId()

        val content = reader.read(
            uriText = uri,
            bucketId = bucketId
        )

        checkAccount(accountId)

        val reservation = writes.executeAs<FileUploadRow>(
            action = BackendAction.FILE_RESERVE,
            payload = buildJsonObject {
                put("project_id", projectId)
                put("bucket_id", bucketId)
                put("file_name", content.fileName)
                put("mime_type", content.mimeType)
                put("size_bytes", content.bytes.size.toLong())
            }
        )

        checkAccount(accountId)

        client.storage.from(bucketId).upload(
            reservation.storagePath,
            content.bytes
        ) {
            upsert = false
            contentType = ContentType.parse(content.mimeType)
        }

        checkAccount(accountId)

        return reservation
    }

    private fun currentAccountId(): String {
        return client.auth.currentUserOrNull()?.id
            ?: throw BackendSessionRequiredException()
    }

    private fun checkAccount(expectedAccountId: String) {
        if (client.auth.currentUserOrNull()?.id != expectedAccountId) {
            throw BackendAccountChangedException()
        }
    }
}