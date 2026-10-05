package com.mcc.construction.data.remote

import org.junit.Test

class BackendFileRulesTest {

    @Test
    fun photoAtMaximumSizeIsAccepted() {
        BackendFileRules.validate(
            bucketId = SupabaseConfig.PHOTO_BUCKET,
            mimeType = "image/jpeg",
            sizeBytes = BackendFileRules.PHOTO_LIMIT_BYTES.toLong()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun photoAboveMaximumSizeIsRejected() {
        BackendFileRules.validate(
            bucketId = SupabaseConfig.PHOTO_BUCKET,
            mimeType = "image/jpeg",
            sizeBytes =
                BackendFileRules.PHOTO_LIMIT_BYTES.toLong() + 1L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun pdfCannotBeUploadedAsPhoto() {
        BackendFileRules.validate(
            bucketId = SupabaseConfig.PHOTO_BUCKET,
            mimeType = "application/pdf",
            sizeBytes = 100L
        )
    }

    @Test
    fun pdfIsAcceptedInDocumentBucket() {
        BackendFileRules.validate(
            bucketId = SupabaseConfig.DOCUMENT_BUCKET,
            mimeType = "application/pdf",
            sizeBytes = 100L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyDocumentIsRejected() {
        BackendFileRules.validate(
            bucketId = SupabaseConfig.DOCUMENT_BUCKET,
            mimeType = "application/pdf",
            sizeBytes = 0L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun executableDocumentIsRejected() {
        BackendFileRules.validate(
            bucketId = SupabaseConfig.DOCUMENT_BUCKET,
            mimeType = "application/x-msdownload",
            sizeBytes = 100L
        )
    }
}