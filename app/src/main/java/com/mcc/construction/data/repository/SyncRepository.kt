package com.mcc.construction.data.repository

import com.mcc.construction.data.model.OfflineSync
import com.mcc.construction.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Every way a manual sync can end. */
sealed interface SyncResult {
    data class Success(val uploaded: Int) : SyncResult

    /** Nothing was attempted because the app is offline. */
    data object Offline : SyncResult

    /** A sync was already running; this request was ignored. */
    data object AlreadySyncing : SyncResult
}

/**
 * Connectivity and the offline upload queue — the data behind every sync
 * indicator in the app.
 *
 * Other repositories save changes locally first and add them to this queue when
 * the device is offline, so a worker underground never loses a report.
 */
interface SyncRepository {

    val status: StateFlow<SyncStatus>

    /** Past sync attempts from this device (OFFLINE_SYNC), newest first. */
    fun observeHistory(): Flow<List<OfflineSync>>

    /**
     * The user's choice to work offline: uploads are held until it is switched
     * off again. Also how the offline path is demonstrated on a connected device.
     */
    suspend fun setOfflineMode(enabled: Boolean)

    /** Uploads everything in the queue. */
    suspend fun syncNow(): SyncResult
}
