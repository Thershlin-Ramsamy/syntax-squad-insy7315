package com.mcc.construction.data.remote

import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MessageOutboxTest {

    private val clock = Clock.fixed(
        Instant.parse("2026-10-05T08:00:00Z"),
        ZoneId.of("Africa/Johannesburg")
    )

    private val accountA =
        "11111111-1111-4111-8111-111111111111"

    private val accountB =
        "22222222-2222-4222-8222-222222222222"

    @Test
    fun restartPreservesOriginalOperationIdentity() = runTest {
        val storage = MemoryStorage()

        val first = MessageOutbox(
            storage,
            backgroundScope,
            clock
        )

        val operation = operation(
            accountA,
            receiverId = 6,
            content = "Site update"
        )

        val queued = first.enqueue(
            senderId = 5,
            receiverId = 6,
            operation = operation
        )

        val restarted = MessageOutbox(
            storage,
            backgroundScope,
            clock
        )

        restarted.awaitReady()

        assertEquals(
            queued,
            restarted.entries.value.single()
        )

        assertEquals(
            operation.operationId,
            restarted.entries.value
                .single().operation.operationId
        )

        assertEquals(
            operation.payload,
            restarted.entries.value
                .single().operation.payload
        )
    }

    @Test
    fun messagesBelongToBothAuthAndApplicationAccount() = runTest {
        val outbox = MessageOutbox(
            MemoryStorage(),
            backgroundScope,
            clock
        )

        val queued = outbox.enqueue(
            senderId = 5,
            receiverId = 6,
            operation = operation(
                accountA,
                receiverId = 6,
                content = "Private message"
            )
        )

        assertTrue(
            queued.belongsTo(
                accountA,
                applicationUserId = 5
            )
        )

        assertFalse(
            queued.belongsTo(
                accountB,
                applicationUserId = 5
            )
        )

        assertFalse(
            queued.belongsTo(
                accountA,
                applicationUserId = 7
            )
        )
    }

    @Test
    fun failedPersistenceDoesNotReportASavedMessage() = runTest {
        val storage = MemoryStorage()

        val outbox = MessageOutbox(
            storage,
            backgroundScope,
            clock
        )

        outbox.awaitReady()

        storage.failWrites = true

        try {
            outbox.enqueue(
                senderId = 5,
                receiverId = 6,
                operation = operation(
                    accountA,
                    receiverId = 6,
                    content = "Must not disappear"
                )
            )

            fail("Expected storage failure.")
        } catch (_: IOException) {
            // Expected.
        }

        assertTrue(
            outbox.entries.value.isEmpty()
        )

        assertTrue(
            storage.messages.isEmpty()
        )
    }

    @Test
    fun acknowledgementRemovesOnlyTheConfirmedOperation() = runTest {
        val storage = MemoryStorage()

        val outbox = MessageOutbox(
            storage,
            backgroundScope,
            clock
        )

        val first = outbox.enqueue(
            senderId = 5,
            receiverId = 6,
            operation = operation(
                accountA,
                receiverId = 6,
                content = "First"
            )
        )

        val second = outbox.enqueue(
            senderId = 7,
            receiverId = 8,
            operation = operation(
                accountB,
                receiverId = 8,
                content = "Second"
            )
        )

        outbox.acknowledge(
            first.operation.operationId
        )

        assertEquals(
            listOf(second),
            outbox.entries.value
        )

        assertEquals(
            listOf(second),
            storage.messages
        )
    }

    @Test
    fun failedAcknowledgementRetainsTheOriginalRetryOperation() = runTest {
        val storage = MemoryStorage()

        val outbox = MessageOutbox(
            storage,
            backgroundScope,
            clock
        )

        val queued = outbox.enqueue(
            senderId = 5,
            receiverId = 6,
            operation = operation(
                accountA,
                receiverId = 6,
                content = "Retry safely"
            )
        )

        storage.failWrites = true

        try {
            outbox.acknowledge(
                queued.operation.operationId
            )

            fail("Expected storage failure.")
        } catch (_: IOException) {
            // Expected.
        }

        assertEquals(
            queued,
            outbox.entries.value.single()
        )
    }

    @Test
    fun serializationPreservesReplayPayload() = runTest {
        val outbox = MessageOutbox(
            MemoryStorage(),
            backgroundScope,
            clock
        )

        val queued = outbox.enqueue(
            senderId = 5,
            receiverId = 6,
            operation = operation(
                accountA,
                receiverId = 6,
                content = "Line one\nLine two"
            )
        )

        val json = Json {
            encodeDefaults = true
        }

        val restored =
            json.decodeFromString<OutboxMessage>(
                json.encodeToString(queued)
            )

        assertEquals(
            queued,
            restored
        )

        assertEquals(
            "Line one\nLine two",
            restored.toMessage().content
        )

        assertTrue(
            restored.toMessage().messageId < 0
        )
    }

    private fun operation(
        accountId: String,
        receiverId: Int,
        content: String
    ): BackendOperation {
        return BackendOperation.create(
            authUserId = accountId,
            action = BackendAction.MESSAGE_SEND,
            payload = buildJsonObject {
                put("receiver_id", receiverId)
                put("content", content)
            }
        )
    }

    private class MemoryStorage : OutboxStorage {

        var messages: List<OutboxMessage> =
            emptyList()

        var failWrites = false

        override suspend fun read():
                List<OutboxMessage> {
            return messages.toList()
        }

        override suspend fun write(
            messages: List<OutboxMessage>
        ) {
            if (failWrites) {
                throw IOException(
                    "Simulated storage failure."
                )
            }

            this.messages = messages.toList()
        }
    }
}