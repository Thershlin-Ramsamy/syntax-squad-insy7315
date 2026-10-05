package com.mcc.construction.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class BackendReadStore(
    private val client: SupabaseClient,
    private val refreshBus: BackendRefreshBus,
    private val refreshIntervalMillis: Long = 15_000L
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
    }

    private val mutableFailedTables =
        MutableStateFlow<Set<String>>(emptySet())

    val failedTables: StateFlow<Set<String>> =
        mutableFailedTables.asStateFlow()

    init {
        require(refreshIntervalMillis >= 1_000L)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTable(
        table: String,
        primaryKey: String
    ): Flow<List<JsonObject>> {
        require(primaryKeys[table] == primaryKey) {
            "Unsupported table or primary key."
        }

        return client.auth.sessionStatus
            .map { client.auth.currentUserOrNull()?.id }
            .distinctUntilChanged()
            .flatMapLatest { accountId ->
                if (accountId == null) {
                    flowOf(emptyList<JsonObject>())
                } else {
                    flow {
                        var previousRows = emptyList<JsonObject>()
                        emit(previousRows)

                        val periodicRefresh = flow {
                            while (true) {
                                emit(Unit)
                                delay(refreshIntervalMillis)
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
                                emit(emptyList())
                                return@collect
                            }

                            try {
                                val rows = fetchTable(table, primaryKey)

                                if (
                                    client.auth.currentUserOrNull()?.id !=
                                    accountId
                                ) {
                                    emit(emptyList())
                                    return@collect
                                }

                                previousRows = rows
                                mutableFailedTables.update {
                                    it - table
                                }

                                emit(rows)
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Exception) {
                                mutableFailedTables.update {
                                    it + table
                                }

                                if (
                                    client.auth.currentUserOrNull()?.id ==
                                    accountId
                                ) {
                                    emit(previousRows)
                                } else {
                                    emit(emptyList())
                                }
                            }
                        }
                    }
                }
            }
    }

    inline fun <reified T> observeRows(
        table: String,
        primaryKey: String
    ): Flow<List<T>> {
        return mapRows(
            table,
            observeTable(table, primaryKey)
        ) { rows ->
            json.decodeFromJsonElement(
                kotlinx.serialization.serializer<List<T>>(),
                JsonArray(rows)
            )
        }
    }

    fun <R, T> mapRows(
        table: String,
        source: Flow<List<R>>,
        transform: suspend (List<R>) -> List<T>
    ): Flow<List<T>> = flow {
        var previous = emptyList<T>()

        source.collect { rows ->
            val mapped = try {
                transform(rows)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableFailedTables.update {
                    it + table
                }
                previous
            }

            previous = mapped
            emit(mapped)
        }
    }

    private suspend fun fetchTable(
        table: String,
        primaryKey: String
    ): List<JsonObject> {
        val rows = mutableListOf<JsonObject>()
        var offset = 0L

        while (true) {
            val page = client.from(table).select {
                order(primaryKey, Order.ASCENDING)
                range(offset..(offset + PAGE_SIZE - 1L))
            }.decodeList<JsonObject>()

            rows.addAll(page)

            if (page.isEmpty()) break

            offset += page.size.toLong()
        }

        return rows.distinctBy { it[primaryKey] }
    }

    companion object {
        private const val PAGE_SIZE = 200L

        private val primaryKeys = mapOf(
            "roles" to "role_id",
            "users" to "user_id",
            "projects" to "project_id",
            "project_members" to "membership_id",
            "tasks" to "task_id",
            "task_assignments" to "assignment_id",
            "task_photos" to "photo_id",
            "attendance" to "attendance_id",
            "daily_reports" to "report_id",
            "suppliers" to "supplier_id",
            "inventory" to "material_id",
            "documents" to "document_id",
            "safety_checklist_items" to "item_id",
            "safety_inspections" to "inspection_id",
            "incident_reports" to "incident_id",
            "notifications" to "notification_id",
            "messages" to "message_id",
            "audit_logs" to "log_id",
            "offline_sync" to "sync_id"
        )
    }
}