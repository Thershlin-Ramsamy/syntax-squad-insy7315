package com.mcc.construction.data.remote

import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MessageOutbox(
    private val storage: OutboxStorage,
    scope: CoroutineScope,
    private val clock: Clock
) {

    private val mutex = Mutex()

    private val mutableEntries =
        MutableStateFlow<List<OutboxMessage>>(emptyList())

    val entries: StateFlow<List<OutboxMessage>> =
        mutableEntries.asStateFlow()

    private val initialization = scope.async {
        mutableEntries.value = storage.read()
    }

    suspend fun awaitReady() {
        initialization.await()
    }

    suspend fun enqueue(
        senderId: Int,
        receiverId: Int,
        operation: BackendOperation
    ): OutboxMessage {
        awaitReady()

        return mutex.withLock {
            check(mutableEntries.value.size < MAX_MESSAGES) {
                "The message queue is full."
            }

            check(
                mutableEntries.value.none {
                    it.operation.operationId == operation.operationId
                }
            ) {
                "This operation is already queued."
            }

            val lowestId = mutableEntries.value
                .minOfOrNull { it.localId } ?: 0

            check(lowestId > Int.MIN_VALUE)

            val entry = OutboxMessage(
                localId = lowestId - 1,
                senderId = senderId,
                receiverId = receiverId,
                queuedAt = LocalDateTime.now(clock).toString(),
                operation = operation
            )

            replace(mutableEntries.value + entry)

            entry
        }
    }

    suspend fun acknowledge(
        operationId: String
    ) {
        awaitReady()

        mutex.withLock {
            val current = mutableEntries.value

            val updated = current.filterNot {
                it.operation.operationId == operationId
            }

            if (updated.size != current.size) {
                replace(updated)
            }
        }
    }

    /**
     * Keep disk and in-memory state together even if the caller is cancelled
     * immediately after an atomic disk write.
     */
    private suspend fun replace(
        entries: List<OutboxMessage>
    ) {
        withContext(NonCancellable) {
            storage.write(entries)
            mutableEntries.value = entries
        }
    }

    companion object {
        private const val MAX_MESSAGES = 500
    }
}