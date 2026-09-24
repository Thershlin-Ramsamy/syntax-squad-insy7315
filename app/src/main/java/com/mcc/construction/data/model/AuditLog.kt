package com.mcc.construction.data.model

import java.time.LocalDateTime

/** A row of the AUDIT_LOGS table. */
data class AuditLog(
    val logId: Int,
    val userId: Int,
    val action: String,
    val timestamp: LocalDateTime
)
