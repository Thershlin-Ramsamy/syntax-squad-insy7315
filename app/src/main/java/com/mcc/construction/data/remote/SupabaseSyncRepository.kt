package com.mcc.construction.data.remote

import android.content.Context
import com.mcc.construction.data.model.OfflineSync
import com.mcc.construction.data.model.PendingChange
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.SyncOutcome
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.remote.dto.OfflineSyncRow
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.SyncResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseSyncRepository(
    context: Context,
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    private val network: BackendNetworkMonitor,
    private val outbox: MessageOutbox,
    private val gateway: BackendGateway,
    private val reads: BackendReadStore,
    scope: CoroutineScope,
    private val clock: Clock
) : SyncRepository {

    private val preferences = context.applicationContext
        .getSharedPreferences(
            "mcc_backend_sync",
            Context.MODE_PRIVATE
        )

    private val deviceId: String =
        preferences.getString("device_id", null)
            ?: UUID.randomUUID().toString().also {
                check(
                    preferences.edit()
                        .putString("device_id", it)
                        .commit()
                )
            }

    private val offline = MutableStateFlow(
        preferences.getBoolean("offline_mode", false)
    )

    private val syncing = MutableStateFlow(false)

    private val lastSynced =
        MutableStateFlow<LocalDateTime?>(null)

    private val mutableLastError =
        MutableStateFlow<String?>(null)

    val lastError: StateFlow<String?> =
        mutableLastError.asStateFlow()

    private val syncMutex = Mutex()

    private val baseStatus = combine(
        network.available,
        offline,
        auth.session,
        outbox.entries
    ) { available, offlineMode, session, entries ->

        val accountId = client.auth.currentUserOrNull()?.id

        val owned = if (
            session == null || accountId == null
        ) {
            emptyList()
        } else {
            entries.filter {
                it.belongsTo(accountId, session.user.userId)
            }
        }

        SyncStatus(
            isNetworkAvailable = available,
            isOfflineMode = offlineMode,
            isSyncing = false,
            pendingChanges = owned.map {
                PendingChange(
                    changeId = it.localId,
                    type = PendingChangeType.MESSAGE,
                    summary = "Message to user ${it.receiverId}",
                    queuedAt = LocalDateTime.parse(it.queuedAt)
                )
            },
            lastSyncedAt = null
        )
    }

    override val status: StateFlow<SyncStatus> = combine(
        baseStatus,
        syncing,
        lastSynced
    ) { base, running, last ->
        base.copy(
            isSyncing = running,
            lastSyncedAt = last
        )
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        SyncStatus.Initial.copy(
            isNetworkAvailable = false
        )
    )

    init {
        // Foreground-process retry. This is not a background WorkManager job.
        scope.launch {
            while (true) {
                try {
                    if (
                        network.available.value &&
                        !offline.value &&
                        auth.session.value != null
                    ) {
                        syncNow()
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // syncNow retains the queue and sets lastError.
                }

                delay(RETRY_MILLIS)
            }
        }
    }

    override fun observeHistory(): Flow<List<OfflineSync>> {
        return reads.observeRows<OfflineSyncRow>(
            table = "offline_sync",
            primaryKey = "sync_id"
        ).map { rows ->
            rows.map {
                OfflineSync(
                    syncId = it.syncId,
                    userId = it.userId,
                    deviceId = it.deviceId,
                    syncStatus = SyncOutcome.valueOf(it.syncStatus),
                    timestamp = BackendTime.timestamp(it.timestamp)
                )
            }.sortedByDescending {
                it.timestamp
            }
        }
    }

    override suspend fun setOfflineMode(
        enabled: Boolean
    ) {
        check(
            preferences.edit()
                .putBoolean("offline_mode", enabled)
                .commit()
        ) {
            "The offline preference could not be saved."
        }

        offline.value = enabled
    }

    override suspend fun syncNow(): SyncResult {
        if (!syncMutex.tryLock()) {
            return SyncResult.AlreadySyncing
        }

        try {
            outbox.awaitReady()

            if (
                !network.available.value ||
                offline.value
            ) {
                return SyncResult.Offline
            }

            val session = auth.session.value
                ?: throw BackendSessionRequiredException()

            val accountId = client.auth.currentUserOrNull()?.id
                ?: throw BackendSessionRequiredException()

            val applicationUserId = session.user.userId

            val pending = outbox.entries.value.filter {
                it.belongsTo(accountId, applicationUserId)
            }

            if (pending.isEmpty()) {
                return SyncResult.Success(0)
            }

            syncing.value = true
            mutableLastError.value = null

            var uploaded = 0

            for (entry in pending) {
                if (
                    !network.available.value ||
                    offline.value
                ) {
                    return SyncResult.Offline
                }

                if (
                    auth.session.value?.user?.userId !=
                    applicationUserId ||
                    client.auth.currentUserOrNull()?.id !=
                    accountId
                ) {
                    throw BackendAccountChangedException()
                }

                // execute() verifies the original Auth UUID again.
                gateway.execute(entry.operation)

                // A crash before this removal is safe: retry uses the same UUID.
                outbox.acknowledge(entry.operation.operationId)

                uploaded++
            }

            lastSynced.value = LocalDateTime.now(clock)

            // Sync history is separate from the completed message uploads.
            try {
                if (
                    auth.session.value?.user?.userId ==
                    applicationUserId &&
                    client.auth.currentUserOrNull()?.id == accountId
                ) {
                    val historyOperation = gateway.prepare(
                        BackendAction.SYNC_RECORD,
                        buildJsonObject {
                            put("device_id", deviceId)
                            put("sync_status", "COMPLETED")
                            put("uploaded_count", uploaded)
                        }
                    )

                    gateway.execute(historyOperation)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableLastError.value =
                    "Messages uploaded; sync history was not confirmed."
            }

            return SyncResult.Success(uploaded)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            mutableLastError.value =
                "Sync could not finish. Pending messages were retained."

            throw exception
        } finally {
            syncing.value = false
            syncMutex.unlock()
        }
    }

    companion object {
        private const val RETRY_MILLIS = 15_000L
    }
}