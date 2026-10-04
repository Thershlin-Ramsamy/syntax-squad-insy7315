package com.mcc.construction.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

class BackendSessionRequiredException :
    IllegalStateException(
        "Sign in before performing this operation."
    )

class BackendAccountChangedException :
    IllegalStateException(
        "This operation belongs to a different signed-in account."
    )

/**
 * Calls the validated PostgreSQL operation gateway.
 *
 * No automatic retries are performed here. A caller retrying a failed
 * network request must reuse the same BackendOperation.
 *
 * Server errors and coroutine cancellation propagate to the caller.
 */
class BackendGateway(
    private val client: SupabaseClient,
    private val refreshBus: BackendRefreshBus
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun prepare(
        action: BackendAction,
        payload: JsonObject
    ): BackendOperation {
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw BackendSessionRequiredException()

        if (client.auth.currentSessionOrNull() == null) {
            throw BackendSessionRequiredException()
        }

        return BackendOperation.create(
            authUserId = userId,
            action = action,
            payload = payload
        )
    }

    suspend fun execute(
        operation: BackendOperation
    ): JsonObject {
        requireMatchingAccount(operation)

        val parameters = buildJsonObject {
            put(
                "p_operation_id",
                operation.operationId
            )
            put(
                "p_action",
                operation.action.name
            )
            put(
                "p_payload",
                operation.payload
            )
        }

        val response = client.postgrest.rpc(
            function = "mcc_execute",
            parameters = parameters
        ).decodeAs<JsonObject>()

        // Avoid applying an old account's response after an Auth switch.
        requireMatchingAccount(operation)

        refreshBus.invalidate()

        return response;
    }

    suspend inline fun <reified T> executeAs(
        operation: BackendOperation
    ): T {
        val response = execute(operation)

        return json.decodeFromJsonElement<T>(response)
    }

    private fun requireMatchingAccount(
        operation: BackendOperation
    ) {
        val currentUserId =
            client.auth.currentUserOrNull()?.id
                ?: throw BackendSessionRequiredException()

        if (client.auth.currentSessionOrNull() == null) {
            throw BackendSessionRequiredException()
        }

        if (currentUserId != operation.authUserId) {
            throw BackendAccountChangedException()
        }
    }
}