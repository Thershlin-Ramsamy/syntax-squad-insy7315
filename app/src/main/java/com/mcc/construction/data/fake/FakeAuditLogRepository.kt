package com.mcc.construction.data.fake

import com.mcc.construction.data.model.AuditLog
import com.mcc.construction.data.repository.AuditLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAuditLogRepository(seed: FakeSeedData) : AuditLogRepository {

    private val entries = seed.auditLog.sortedByDescending { it.timestamp }

    override fun observeAuditLog(): Flow<List<AuditLog>> = flowOf(entries)
}
