package com.mcc.construction.data.remote

import com.mcc.construction.data.remote.dto.UserRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject

/**
 * Shared helpers for repositories using the mcc_execute RPC.
 *
 * Supabase Auth UUIDs and application integer user IDs are different IDs.
 * Never substitute one for the other.
 */
class BackendRepositorySupport(
    private val client: SupabaseClient,
    val gateway: BackendGateway
) {
    suspend fun currentApplicationUserId(): Int {
        val authUserId = client.auth.currentUserOrNull()?.id
            ?: throw BackendSessionRequiredException()

        val users = client.from("users").select {
            filter {
                eq("auth_user_id", authUserId)
            }
        }.decodeList<UserRow>()

        if (client.auth.currentUserOrNull()?.id != authUserId) {
            throw BackendAccountChangedException()
        }

        val user = users.singleOrNull()
            ?: error("The signed-in account has no application user profile.")

        check(user.isActive) {
            "This application account is suspended."
        }

        return user.userId
    }

    suspend fun requireCurrentUser(userId: Int) {
        require(userId > 0) {
            "A positive application user ID is required."
        }

        check(currentApplicationUserId() == userId) {
            "This operation must use the signed-in application user."
        }
    }

    suspend fun execute(
        action: BackendAction,
        payload: JsonObject
    ): JsonObject {
        val operation = gateway.prepare(action, payload)
        return gateway.execute(operation)
    }

    suspend inline fun <reified T> executeAs(
        action: BackendAction,
        payload: JsonObject
    ): T {
        val operation = gateway.prepare(action, payload)
        return gateway.executeAs<T>(operation)
    }
}