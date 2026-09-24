package com.mcc.construction.data.model

import java.time.LocalDateTime

/** A row of the DOCUMENTS table. */
data class Document(
    val documentId: Int,
    val projectId: Int,
    val fileName: String,
    val version: Int,
    val uploadDate: LocalDateTime
) {
    /** Upper-case file extension, e.g. "PDF", or an empty string if there is none. */
    val extension: String
        get() = fileName.substringAfterLast('.', missingDelimiterValue = "").uppercase()
}
