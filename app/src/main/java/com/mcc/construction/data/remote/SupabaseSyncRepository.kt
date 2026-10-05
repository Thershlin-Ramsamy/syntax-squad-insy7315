package com.mcc.construction.data.remote

import android.content.Context
import com.mcc.construction.data.model.*
import com.mcc.construction.data.remote.dto.OfflineSyncRow
import com.mcc.construction.data.repository.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
    private val clock: Clock,
    private val organisation: SystemSettingsRepository? = null
) : SyncRepository {

    private val preferences = context.applicationContext
        .getSharedPreferences(
            "mcc_backend_sync",
            Context.MODE_PRIVATE
        )

    private val deviceId =
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
    private val lastSynced = MutableStateFlow<LocalDateTime?>(null)
    private val mutableLastError = MutableStateFlow<String?>(null)

    val lastError: StateFlow<String?> =
        mutableLastError.asStateFlow()

    private val syncMutex = Mutex()

    private val settingsFlow: StateFlow<SystemSettings> =
        organisation?.settings ?: MutableStateFlow(SystemSettings())

    private val baseStatus = combine(
        network.available,
        offline,
        auth.session,
        outbox.entries
    ) { available, offlineMode, session, entries ->
        val accountId = client.auth.currentUserOrNull()?.id
        val owned = if (session == null || accountId == null) {
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

    override val status: StateFlow<SyncStatus> =
        combine(baseStatus, syncing, lastSynced) { base, running, last ->
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
        scope.launch {
            combine(
                AppForeground.active,
                auth.session.map { it?.user?.userId }.distinctUntilChanged(),
                settingsFlow.map {
                    it.syncIntervalMinutes
                }.distinctUntilChanged(),
                combine(
                    network.available,
                    offline
                ) { available, offlineMode ->
                    available && !offlineMode
                }.distinctUntilChanged()
            ) { foreground, userId, interval, online ->
                ScheduleState(
                    enabled = foreground && userId != null && online,
                    userId = userId,
                    intervalMinutes = interval
                )
            }.distinctUntilChanged().collectLatest { schedule ->
                if (!schedule.enabled) return@collectLatest

                while (true) {
                    try {
                        syncNow()
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        // The queue is retained; retry at the configured interval.
                    }

                    delay(
                        SyncSchedule.intervalMillis(
                            schedule.intervalMinutes
                        )
                    )
                }
            }
        }
    }

    override fun observeHistory(): Flow<List<OfflineSync>> =
        reads.observeRows<OfflineSyncRow>(
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
            }.sortedByDescending { it.timestamp }
        }

    override suspend fun setOfflineMode(enabled: Boolean) {
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

            if (!network.available.value || offline.value) {
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
                mutableLastError.value = null
                return SyncResult.Success(0)
            }

            syncing.value = true
            mutableLastError.value = null
            var uploaded = 0

            for (entry in pending) {
                if (!network.available.value || offline.value) {
                    return SyncResult.Offline
                }

                if (
                    auth.session.value?.user?.userId != applicationUserId ||
                    client.auth.currentUserOrNull()?.id != accountId
                ) {
                    throw BackendAccountChangedException()
                }

                gateway.execute(entry.operation)
                outbox.acknowledge(entry.operation.operationId)
                uploaded++
            }

            lastSynced.value = LocalDateTime.now(clock)

            try {
                if (
                    auth.session.value?.user?.userId == applicationUserId &&
                    client.auth.currentUserOrNull()?.id == accountId
                ) {
                    gateway.execute(
                        gateway.prepare(
                            BackendAction.SYNC_RECORD,
                            buildJsonObject {
                                put("device_id", deviceId)
                                put("sync_status", "COMPLETED")
                                put("uploaded_count", uploaded)
                            }
                        )
                    )
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

    private data class ScheduleState(
        val enabled: Boolean,
        val userId: Int?,
        val intervalMinutes: Int
    )
}