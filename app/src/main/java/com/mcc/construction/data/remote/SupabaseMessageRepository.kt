package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Conversation
import com.mcc.construction.data.model.Message
import com.mcc.construction.data.remote.dto.MessageRow
import com.mcc.construction.data.repository.MessageRepository
import com.mcc.construction.data.repository.UserRepository
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseMessageRepository(
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport,
    private val users: UserRepository
) : MessageRepository {

    private fun observeMessages(): Flow<List<Message>> {
        return reads.observeRows<MessageRow>(
            table = "messages",
            primaryKey = "message_id"
        ).map { rows ->
            rows.map { row ->
                mapMessage(row)
            }.sortedWith(
                compareBy<Message> {
                    it.timestamp
                }.thenBy {
                    it.messageId
                }
            )
        }
    }

    override fun observeConversations(
        userId: Int
    ): Flow<List<Conversation>> {
        return combine(
            observeMessages(),
            users.observeUsers()
        ) { messages, directory ->
            val usersById = directory.associateBy {
                it.userId
            }

            messages.filter {
                it.senderId == userId ||
                        it.receiverId == userId
            }.groupBy { message ->
                if (message.senderId == userId) {
                    message.receiverId
                } else {
                    message.senderId
                }
            }.mapNotNull { (otherUserId, thread) ->
                val participant = usersById[otherUserId]
                    ?: return@mapNotNull null

                val latest = thread.maxWithOrNull(
                    compareBy<Message> {
                        it.timestamp
                    }.thenBy {
                        it.messageId
                    }
                ) ?: return@mapNotNull null

                Conversation(
                    participant = participant,
                    lastMessage = latest,
                    unreadCount = thread.count {
                        it.receiverId == userId &&
                                !it.isRead
                    }
                )
            }.sortedWith(
                compareByDescending<Conversation> {
                    it.lastMessage.timestamp
                }.thenByDescending {
                    it.lastMessage.messageId
                }
            )
        }
    }

    override fun observeThread(
        userId: Int,
        otherUserId: Int
    ): Flow<List<Message>> {
        return observeMessages().map { messages ->
            messages.filter { message ->
                (
                        message.senderId == userId &&
                                message.receiverId == otherUserId
                        ) || (
                        message.senderId == otherUserId &&
                                message.receiverId == userId
                        )
            }
        }
    }

    override suspend fun sendMessage(
        senderId: Int,
        receiverId: Int,
        content: String
    ): Message {
        writes.requireCurrentUser(senderId)

        require(receiverId > 0) {
            "A positive recipient ID is required."
        }

        require(receiverId != senderId) {
            "Choose another user as the message recipient."
        }

        val trimmedContent = content.trim()

        require(trimmedContent.isNotEmpty()) {
            "A message cannot be empty."
        }

        val row = writes.executeAs<MessageRow>(
            action = BackendAction.MESSAGE_SEND,
            payload = buildJsonObject {
                put("receiver_id", receiverId)
                put("content", trimmedContent)
            }
        )

        return mapMessage(row)
    }

    override fun observeUnreadCount(
        userId: Int
    ): Flow<Int> {
        return observeMessages().map { messages ->
            messages.count {
                it.receiverId == userId &&
                        !it.isRead
            }
        }
    }

    override suspend fun markThreadRead(
        userId: Int,
        otherUserId: Int
    ) {
        writes.requireCurrentUser(userId)

        require(otherUserId > 0) {
            "A positive participant ID is required."
        }

        require(otherUserId != userId) {
            "Choose another conversation participant."
        }

        writes.execute(
            action = BackendAction.THREAD_READ,
            payload = buildJsonObject {
                put("other_user_id", otherUserId)
            }
        )
    }

    private fun mapMessage(
        row: MessageRow
    ): Message {
        return Message(
            messageId = row.messageId,
            senderId = row.senderId,
            receiverId = row.receiverId,
            content = row.content,
            timestamp = OffsetDateTime.parse(row.timestamp)
                .atZoneSameInstant(SOUTH_AFRICA_ZONE)
                .toLocalDateTime(),
            isRead = row.isRead
        )
    }

    companion object {
        private val SOUTH_AFRICA_ZONE =
            ZoneId.of("Africa/Johannesburg")
    }
}