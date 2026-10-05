package com.mcc.construction.data.remote

import com.mcc.construction.data.model.SystemSettings
import com.mcc.construction.data.remote.dto.SystemSettingsRow
import com.mcc.construction.data.repository.SystemSettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseSystemSettingsRepository(
    private val client: SupabaseClient,
    private val writes: BackendRepositorySupport,
    private val refreshBus: BackendRefreshBus,
    scope: CoroutineScope
) : SystemSettingsRepository {

    private val mutableReadFailed = MutableStateFlow(false)

    val readFailed: StateFlow<Boolean> =
        mutableReadFailed.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val settings: StateFlow<SystemSettings> =
        client.auth.sessionStatus
            .map {
                client.auth.currentUserOrNull()?.id
            }
            .distinctUntilChanged()
            .flatMapLatest { accountId ->
                if (accountId == null) {
                    mutableReadFailed.value = false
                    flowOf(SystemSettings())
                } else {
                    flow {
                        var lastSuccessful = SystemSettings()

                        mutableReadFailed.value = false
                        emit(lastSuccessful)

                        val periodicRefresh = flow {
                            while (true) {
                                emit(Unit)
                                delay(15_000L)
                            }
                        }

                        merge(
                            periodicRefresh,
                            refreshBus.revision.map { Unit }
                        ).collect {
                            if (
                                client.auth.currentUserOrNull()?.id !=
                                accountId
                            ) {
                                emit(SystemSettings())
                                return@collect
                            }

                            try {
                                val row = client.from("system_settings")
                                    .select()
                                    .decodeList<SystemSettingsRow>()
                                    .singleOrNull()
                                    ?: error(
                                        "Organisation settings are missing."
                                    )

                                if (
                                    client.auth.currentUserOrNull()?.id !=
                                    accountId
                                ) {
                                    emit(SystemSettings())
                                    return@collect
                                }

                                lastSuccessful = mapSettings(row)
                                mutableReadFailed.value = false

                                emit(lastSuccessful)
                            } catch (
                                exception: CancellationException
                            ) {
                                throw exception
                            } catch (_: Exception) {
                                mutableReadFailed.value = true

                                if (
                                    client.auth.currentUserOrNull()?.id ==
                                    accountId
                                ) {
                                    emit(lastSuccessful)
                                } else {
                                    emit(SystemSettings())
                                }
                            }
                        }
                    }
                }
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = SystemSettings()
            )

    override suspend fun update(
        settings: SystemSettings
    ) {
        require(
            settings.sessionTimeoutMinutes in
                    SystemSettings.SESSION_TIMEOUT_OPTIONS
        ) {
            "Unsupported session timeout."
        }

        require(
            settings.syncIntervalMinutes in
                    SystemSettings.SYNC_INTERVAL_OPTIONS
        ) {
            "Unsupported sync interval."
        }

        writes.execute(
            action = BackendAction.SYSTEM_SETTINGS,
            payload = buildJsonObject {
                put(
                    "session_timeout_minutes",
                    settings.sessionTimeoutMinutes
                )
                put(
                    "require_daily_checklist",
                    settings.requireDailyChecklist
                )
                put(
                    "allow_offline_mode",
                    settings.allowOfflineMode
                )
                put(
                    "low_stock_alerts",
                    settings.lowStockAlerts
                )
                put(
                    "sync_interval_minutes",
                    settings.syncIntervalMinutes
                )
            }
        )
    }

    private fun mapSettings(
        row: SystemSettingsRow
    ): SystemSettings {
        return SystemSettings(
            sessionTimeoutMinutes = row.sessionTimeoutMinutes,
            requireDailyChecklist = row.requireDailyChecklist,
            allowOfflineMode = row.allowOfflineMode,
            lowStockAlerts = row.lowStockAlerts,
            syncIntervalMinutes = row.syncIntervalMinutes
        )
    }
}