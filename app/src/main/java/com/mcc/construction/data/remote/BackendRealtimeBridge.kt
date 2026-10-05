package com.mcc.construction.data.remote

import com.mcc.construction.data.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackendRealtimeBridge(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository,
    private val refreshBus: BackendRefreshBus,
    scope: CoroutineScope
) {

    private val mutableSubscriptionFailed =
        MutableStateFlow(false)

    val subscriptionFailed: StateFlow<Boolean> =
        mutableSubscriptionFailed.asStateFlow()

    init {
        scope.launch {
            authRepository.session
                .map { it?.user?.userId }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    mutableSubscriptionFailed.value = false

                    if (userId != null) {
                        listenUntilCancelled(userId)
                    }
                }
        }
    }

    private suspend fun listenUntilCancelled(
        userId: Int
    ) {
        while (true) {
            val channel = client.channel(
                "mcc-$userId-${UUID.randomUUID()}"
            )

            try {
                coroutineScope {
                    tables.forEach { tableName ->
                        launch {
                            channel.postgresChangeFlow<PostgresAction>(
                                schema = "public"
                            ) {
                                table = tableName
                            }.collect {
                                // Read the permitted rows again rather
                                // than copying event payloads into UI state.
                                refreshBus.invalidate()
                            }
                        }
                    }

                    channel.subscribe(
                        blockUntilSubscribed = true
                    )

                    mutableSubscriptionFailed.value = false
                    refreshBus.invalidate()

                    awaitCancellation()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableSubscriptionFailed.value = true
            } finally {
                withContext(NonCancellable) {
                    try {
                        client.realtime.removeChannel(channel)
                    } catch (_: Exception) {
                        // The transport may already be disconnected.
                    }
                }
            }

            delay(RETRY_MILLIS)
        }
    }

    companion object {
        private const val RETRY_MILLIS = 5_000L

        private val tables = listOf(
            "users",
            "projects",
            "project_members",
            "tasks",
            "task_assignments",
            "task_photos",
            "attendance",
            "daily_reports",
            "suppliers",
            "inventory",
            "documents",
            "safety_checklist_items",
            "safety_inspections",
            "incident_reports",
            "notifications",
            "messages",
            "audit_logs",
            "system_settings"
        )
    }
}