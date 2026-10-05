package com.mcc.construction.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.*

class BackendSessionRequiredException :
    IllegalStateException(
        "Sign in before performing this operation."
    )

class BackendAccountChangedException :
    IllegalStateException(
        "This operation belongs to a different signed-in account."
    )

class BackendGateway(
    private val client: SupabaseClient,
    private val refreshBus: BackendRefreshBus
) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Volatile
    private var writeGuard: () -> Unit = {}

    /**
     * Installed by the real application container before exposing
     * repositories to the UI.
     */
    fun setWriteGuard(guard: () -> Unit) {
        writeGuard = guard
    }

    fun prepare(
        action: BackendAction,
        payload: JsonObject
    ): BackendOperation {
        val accountId = client.auth.currentUserOrNull()?.id
            ?: throw BackendSessionRequiredException()

        if (client.auth.currentSessionOrNull() == null) {
            throw BackendSessionRequiredException()
        }

        return BackendOperation.create(
            authUserId = accountId,
            action = action,
            payload = payload
        )
    }

    suspend fun execute(
        operation: BackendOperation
    ): JsonObject {
        requireMatchingAccount(operation)
        writeGuard()

        val parameters = buildJsonObject {
            put("p_operation_id", operation.operationId)
            put("p_action", operation.action.name)
            put("p_payload", operation.payload)
        }

        val response = client.postgrest.rpc(
            function = "mcc_execute",
            parameters = parameters
        ).decodeAs<JsonObject>()

        requireMatchingAccount(operation)
        refreshBus.invalidate()

        return response
    }

    suspend inline fun <reified T> executeAs(
        operation: BackendOperation
    ): T {
        return json.decodeFromJsonElement<T>(
            execute(operation)
        )
    }

    private fun requireMatchingAccount(
        operation: BackendOperation
    ) {
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: throw BackendSessionRequiredException()

        if (client.auth.currentSessionOrNull() == null) {
            throw BackendSessionRequiredException()
        }

        if (currentUserId != operation.authUserId) {
            throw BackendAccountChangedException()
        }
    }
}