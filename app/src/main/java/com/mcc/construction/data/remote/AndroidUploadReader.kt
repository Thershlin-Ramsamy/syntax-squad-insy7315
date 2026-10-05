package com.mcc.construction.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UploadContent(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

object BackendFileRules {

    const val PHOTO_LIMIT_BYTES = 10 * 1024 * 1024
    const val DOCUMENT_LIMIT_BYTES = 20 * 1024 * 1024

    private val photoMimeTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    private val documentMimeTypes = setOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )

    fun limit(bucketId: String): Int {
        return when (bucketId) {
            SupabaseConfig.PHOTO_BUCKET -> PHOTO_LIMIT_BYTES
            SupabaseConfig.DOCUMENT_BUCKET -> DOCUMENT_LIMIT_BYTES
            else -> error("Unsupported storage bucket.")
        }
    }

    fun validate(
        bucketId: String,
        mimeType: String,
        sizeBytes: Long
    ) {
        val allowedTypes = when (bucketId) {
            SupabaseConfig.PHOTO_BUCKET -> photoMimeTypes
            SupabaseConfig.DOCUMENT_BUCKET -> documentMimeTypes
            else -> error("Unsupported storage bucket.")
        }

        require(mimeType in allowedTypes) {
            "This file type is not supported."
        }

        require(sizeBytes > 0L) {
            "The selected file is empty."
        }

        require(sizeBytes <= limit(bucketId).toLong()) {
            "The selected file exceeds the upload limit."
        }
    }
}

class AndroidUploadReader(
    context: Context
) {

    private val resolver =
        context.applicationContext.contentResolver

    suspend fun read(
        uriText: String,
        bucketId: String
    ): UploadContent = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriText)

        require(uri.scheme == "content") {
            "Select a file using the Android file or photo picker."
        }

        val mimeType = resolver.getType(uri)
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?: error("The selected file has no recognised MIME type.")

        val maximumBytes = BackendFileRules.limit(bucketId)

        var displayName: String? = null
        var declaredSize: Long? = null

        resolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                val sizeIndex = cursor.getColumnIndex(
                    OpenableColumns.SIZE
                )

                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    displayName = cursor.getString(nameIndex)
                }

                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    declaredSize = cursor.getLong(sizeIndex)
                }
            }
        }

        declaredSize?.let { size ->
            require(size <= maximumBytes.toLong()) {
                "The selected file exceeds the upload limit."
            }
        }

        val fileName = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: error("The selected file has no filename.")

        require(fileName.length <= 255) {
            "The filename is too long."
        }

        require(fileName.none { it.isISOControl() }) {
            "The filename contains unsupported characters."
        }

        val input = resolver.openInputStream(uri)
            ?: throw IOException("The selected file could not be opened.")

        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L

            while (true) {
                val count = stream.read(buffer)

                if (count < 0) {
                    break
                }

                if (count == 0) {
                    continue
                }

                total += count

                require(total <= maximumBytes.toLong()) {
                    "The selected file exceeds the upload limit."
                }

                output.write(buffer, 0, count)
            }

            output.toByteArray()
        }

        BackendFileRules.validate(
            bucketId = bucketId,
            mimeType = mimeType,
            sizeBytes = bytes.size.toLong()
        )

        UploadContent(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
    }
}