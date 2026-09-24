package com.mcc.construction.data.model

import java.time.LocalDateTime

/**
 * What kind of document it is. Not in the ERD yet — see "Fields beyond the
 * ERD" in docs/ARCHITECTURE.md.
 */
enum class DocumentCategory { DRAWING, PERMIT, CONTRACT, SAFETY, REPORT, OTHER }

/** A row of the DOCUMENTS table. */
data class Document(
    val documentId: Int,
    val projectId: Int,
    val fileName: String,
    val version: Int,
    val uploadDate: LocalDateTime,
    val category: DocumentCategory = DocumentCategory.OTHER
) {
    /** Upper-case file extension, e.g. "PDF", or an empty string if there is none. */
    val extension: String
        get() = fileName.substringAfterLast('.', missingDelimiterValue = "").uppercase()
}
