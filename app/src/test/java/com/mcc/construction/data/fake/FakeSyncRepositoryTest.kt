package com.mcc.construction.data.fake

import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.SyncOutcome
import com.mcc.construction.data.model.SyncState
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.repository.SyncResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeSyncRepositoryTest {

    private val container = testContainer()
    private val sync = container.syncRepository

    @Test
    fun startsOnlineWithSeededChangesWaiting() {
        val status = sync.status.value

        assertEquals(SyncState.PENDING, status.state)
        assertEquals(2, status.pendingCount)
    }

    @Test
    fun syncNowUploadsEverythingAndRecordsHistory() = runTest {
        container.authRepository.signIn("worker@mcc.co.za", "pw")
        val historyBefore = sync.observeHistory().first().size

        val result = sync.syncNow()

        assertEquals(SyncResult.Success(uploaded = 2), result)
        assertEquals(SyncState.SYNCED, sync.status.value.state)
        val history = sync.observeHistory().first()
        assertEquals(historyBefore + 1, history.size)
        assertEquals(SyncOutcome.COMPLETED, history.first().syncStatus)
    }

    @Test
    fun offlineModeBlocksSyncAndKeepsTheQueue() = runTest {
        sync.setOfflineMode(true)

        val result = sync.syncNow()

        assertEquals(SyncResult.Offline, result)
        assertEquals(SyncState.OFFLINE, sync.status.value.state)
        assertEquals(2, sync.status.value.pendingCount)
    }

    @Test
    fun changesMadeOfflineAreQueuedThenUploaded() = runTest {
        sync.setOfflineMode(true)
        container.taskRepository.updateStatus(taskId = 102, status = TaskStatus.IN_PROGRESS)
        container.attendanceRepository.clockIn(userId = 5)

        val queued = sync.status.value.pendingChanges
        assertEquals(4, queued.size)
        assertEquals(PendingChangeType.TASK_UPDATE, queued[2].type)
        assertEquals("Erect scaffolding — east elevation", queued[2].summary)
        assertEquals(PendingChangeType.CLOCK_IN, queued[3].type)

        sync.setOfflineMode(false)
        assertEquals(SyncResult.Success(uploaded = 4), sync.syncNow())
        assertEquals(0, sync.status.value.pendingCount)
    }

    @Test
    fun changesMadeOnlineAreNotQueued() = runTest {
        container.taskRepository.updateStatus(taskId = 102, status = TaskStatus.COMPLETED)

        assertEquals(2, sync.status.value.pendingCount)
        assertEquals(FIXED_NOW, sync.status.value.lastSyncedAt)
    }
}
