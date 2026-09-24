package com.mcc.construction.data.fake

import com.mcc.construction.data.model.OfflineSync
import com.mcc.construction.data.model.PendingChange
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.SyncOutcome
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.SyncResult
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

/**
 * In-memory [SyncRepository].
 *
 * The network is always "available"; offline is reached through offline mode,
 * which is exactly the toggle the demo needs. While online, a change recorded
 * with [recordChange] counts as uploaded straight away. While offline it joins
 * the queue and stays there until [syncNow] runs — deliberately not automatic,
 * so the queue can be shown before it is cleared.
 *
 * @param syncDuration how long a sync appears to take, so the "Syncing" state
 *   is visible. Tests pass [Duration.ZERO].
 * @param currentUserId whose name goes on the OFFLINE_SYNC history rows.
 */
class FakeSyncRepository(
    seed: FakeSeedData,
    private val clock: Clock,
    private val syncDuration: Duration,
    private val currentUserId: () -> Int?
) : SyncRepository {

    private val _status = MutableStateFlow(
        SyncStatus(
            isNetworkAvailable = true,
            isOfflineMode = false,
            isSyncing = false,
            pendingChanges = seed.pendingChanges,
            lastSyncedAt = seed.lastSyncedAt
        )
    )
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val history = MutableStateFlow(seed.syncHistory)
    private val nextChangeId = AtomicInteger(seed.pendingChanges.maxOf { it.changeId } + 1)
    private val nextSyncId = AtomicInteger(seed.syncHistory.maxOf { it.syncId } + 1)
    private val syncLock = Mutex()

    override fun observeHistory(): Flow<List<OfflineSync>> = history

    override suspend fun setOfflineMode(enabled: Boolean) {
        _status.update { it.copy(isOfflineMode = enabled) }
    }

    override suspend fun syncNow(): SyncResult {
        if (!_status.value.isOnline) return SyncResult.Offline
        if (!syncLock.tryLock()) return SyncResult.AlreadySyncing
        try {
            _status.update { it.copy(isSyncing = true) }
            delay(syncDuration)

            // Going offline mid-sync leaves the queue intact, as a dropped
            // connection would.
            if (!_status.value.isOnline) {
                logAttempt(SyncOutcome.FAILED)
                _status.update { it.copy(isSyncing = false) }
                return SyncResult.Offline
            }

            val uploaded = _status.value.pendingCount
            val finishedAt = now()
            _status.update {
                it.copy(isSyncing = false, pendingChanges = emptyList(), lastSyncedAt = finishedAt)
            }
            logAttempt(SyncOutcome.COMPLETED)
            return SyncResult.Success(uploaded)
        } finally {
            syncLock.unlock()
        }
    }

    /**
     * Called by the other fake repositories after every local write, standing in
     * for the upload a real repository would attempt.
     */
    internal fun recordChange(type: PendingChangeType, summary: String) {
        val recordedAt = now()
        val change = PendingChange(
            changeId = nextChangeId.getAndIncrement(),
            type = type,
            summary = summary,
            queuedAt = recordedAt
        )
        _status.update { status ->
            if (status.isOnline) {
                status.copy(lastSyncedAt = recordedAt)
            } else {
                status.copy(pendingChanges = status.pendingChanges + change)
            }
        }
    }

    private fun logAttempt(outcome: SyncOutcome) {
        val userId = currentUserId() ?: return
        val entry = OfflineSync(
            syncId = nextSyncId.getAndIncrement(),
            userId = userId,
            deviceId = FakeSeedData.DEMO_DEVICE_ID,
            syncStatus = outcome,
            timestamp = now()
        )
        history.update { listOf(entry) + it }
    }

    private fun now(): LocalDateTime = LocalDateTime.now(clock)
}
