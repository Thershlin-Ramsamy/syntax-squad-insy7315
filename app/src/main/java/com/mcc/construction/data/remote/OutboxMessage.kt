package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Message
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class OutboxMessage(
    val localId: Int,
    val senderId: Int,
    val receiverId: Int,
    val queuedAt: String,
    val operation: BackendOperation
) {

    init {
        require(localId < 0)
        require(senderId > 0)
        require(receiverId > 0)
        require(senderId != receiverId)

        require(operation.action == BackendAction.MESSAGE_SEND)

        require(
            operation.payload["receiver_id"]
                ?.jsonPrimitive
                ?.int == receiverId
        )

        val content = requireNotNull(
            operation.payload["content"]
                ?.jsonPrimitive
                ?.content
        )

        require(content.trim().length in 1..5000)

        LocalDateTime.parse(queuedAt)
    }

    fun belongsTo(
        authUserId: String,
        applicationUserId: Int
    ): Boolean {
        return operation.authUserId == authUserId &&
                senderId == applicationUserId
    }

    fun toMessage(): Message {
        return Message(
            messageId = localId,
            senderId = senderId,
            receiverId = receiverId,
            content = requireNotNull(
                operation.payload["content"]
            ).jsonPrimitive.content,
            timestamp = LocalDateTime.parse(queuedAt),
            isRead = false
        )
    }
}

/**
 * Implementations must replace the complete stored list atomically.
 *
 * A write failure must propagate rather than pretend the message was saved.
 */
interface OutboxStorage {

    suspend fun read(): List<OutboxMessage>

    suspend fun write(messages: List<OutboxMessage>)
}