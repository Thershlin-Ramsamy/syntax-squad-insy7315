package com.mcc.construction.data.fake

import com.mcc.construction.data.model.AuditLog
import com.mcc.construction.data.repository.AuditLogRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAuditLogRepository(
    seed: FakeSeedData,
    private val clock: Clock
) : AuditLogRepository {

    private val entries = MutableStateFlow(seed.auditLog)
    private val nextId = AtomicInteger(seed.auditLog.maxOf { it.logId } + 1)

    override fun observeAuditLog(): Flow<List<AuditLog>> =
        entries.map { list -> list.sortedByDescending { it.timestamp } }

    override suspend fun record(userId: Int, action: String): AuditLog {
        val entry = AuditLog(nextId.getAndIncrement(), userId, action, LocalDateTime.now(clock))
        entries.update { it + entry }
        return entry
    }
}
