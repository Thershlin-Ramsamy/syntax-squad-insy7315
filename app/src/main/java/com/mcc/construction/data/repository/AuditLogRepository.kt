package com.mcc.construction.data.repository

import com.mcc.construction.data.model.AuditLog
import kotlinx.coroutines.flow.Flow

interface AuditLogRepository {

    /** Every audit entry, newest first. */
    fun observeAuditLog(): Flow<List<AuditLog>>

    /** Mirrors the class diagram's `AuditLog.recordAction()`. */
    suspend fun record(userId: Int, action: String): AuditLog
}
