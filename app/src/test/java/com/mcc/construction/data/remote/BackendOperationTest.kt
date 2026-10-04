package com.mcc.construction.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackendOperationTest {
    private val accountId =
        "0b63f644-208f-4a6d-a58b-12729c91d6e5"

    @Test
    fun queuedOperationPreservesRetryIdentityAndPayload() {
        val operation = BackendOperation.create(
            authUserId = accountId,
            action = BackendAction.MESSAGE_SEND,
            payload = buildJsonObject {
                put("receiver_id", 4)
                put("content", "Delivery has arrived.")
                put(
                    "occurred_at",
                    "2026-10-04T08:30:00Z"
                )
            }
        )

        val stored = Json.encodeToString(operation)

        val restored =
            Json.decodeFromString<BackendOperation>(stored)

        assertEquals(operation, restored)
        assertEquals(accountId, restored.authUserId)
    }

    @Test
    fun separateChangesReceiveDifferentOperationIds() {
        val payload = buildJsonObject {
            put("other_user_id", 4)
        }

        val first = BackendOperation.create(
            authUserId = accountId,
            action = BackendAction.THREAD_READ,
            payload = payload
        )

        val second = BackendOperation.create(
            authUserId = accountId,
            action = BackendAction.THREAD_READ,
            payload = payload
        )

        assertNotEquals(
            first.operationId,
            second.operationId
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidOperationIdIsRejected() {
        BackendOperation(
            operationId = "invalid",
            authUserId = accountId,
            action = BackendAction.NOTIFICATIONS_READ,
            payload = buildJsonObject {}
        )
    }
}