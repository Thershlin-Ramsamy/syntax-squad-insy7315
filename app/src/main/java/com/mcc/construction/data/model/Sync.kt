package com.mcc.construction.data.model

import java.time.LocalDateTime

/** Outcome recorded in `OFFLINE_SYNC.SyncStatus`. */
enum class SyncOutcome { COMPLETED, FAILED }

/** A row of the OFFLINE_SYNC table: one sync attempt from one device. */
data class OfflineSync(
    val syncId: Int,
    val userId: Int,
    val deviceId: String,
    val syncStatus: SyncOutcome,
    val timestamp: LocalDateTime
)

/** The kind of local change waiting in the upload queue. */
enum class PendingChangeType {
    CLOCK_IN,
    CLOCK_OUT,
    DAILY_REPORT,
    TASK_UPDATE,
    SAFETY_INSPECTION,
    INCIDENT_REPORT,
    MESSAGE
}

/**
 * A change saved on the device but not yet uploaded. Client-side only — this is
 * the offline queue, not a server table.
 *
 * [summary] is data (a project name, a task title), not a sentence: the UI adds
 * the wording from string resources based on [type].
 */
data class PendingChange(
    val changeId: Int,
    val type: PendingChangeType,
    val summary: String,
    val queuedAt: LocalDateTime
)

/** The single state shown by every sync indicator in the app. */
enum class SyncState {
    /** Online and nothing waiting to upload. */
    SYNCED,

    /** Online with changes waiting — a sync has not run yet. */
    PENDING,

    /** An upload is in progress. */
    SYNCING,

    /** No connection, or the user has switched on offline mode. */
    OFFLINE
}

/**
 * Everything the UI needs to describe connectivity and the upload queue.
 *
 * [isNetworkAvailable] is what the device reports; [isOfflineMode] is the
 * user's choice to hold uploads. Either one keeps the app offline.
 */
data class SyncStatus(
    val isNetworkAvailable: Boolean,
    val isOfflineMode: Boolean,
    val isSyncing: Boolean,
    val pendingChanges: List<PendingChange>,
    val lastSyncedAt: LocalDateTime?
) {
    val isOnline: Boolean get() = isNetworkAvailable && !isOfflineMode
    val pendingCount: Int get() = pendingChanges.size

    val state: SyncState
        get() = when {
            isSyncing -> SyncState.SYNCING
            !isOnline -> SyncState.OFFLINE
            pendingChanges.isNotEmpty() -> SyncState.PENDING
            else -> SyncState.SYNCED
        }

    companion object {
        /** A neutral placeholder for before the repository has emitted. */
        val Initial = SyncStatus(
            isNetworkAvailable = true,
            isOfflineMode = false,
            isSyncing = false,
            pendingChanges = emptyList(),
            lastSyncedAt = null
        )
    }
}
