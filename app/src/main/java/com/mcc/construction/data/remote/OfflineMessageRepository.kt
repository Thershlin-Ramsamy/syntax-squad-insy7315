package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Conversation
import com.mcc.construction.data.model.Message
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.MessageRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OfflineMessageRepository(
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    private val delegate: MessageRepository,
    private val users: UserRepository,
    private val outbox: MessageOutbox,
    private val gateway: BackendGateway,
    private val sync: SyncRepository
) : MessageRepository {

    override fun observeThread(
        userId: Int,
        otherUserId: Int
    ): Flow<List<Message>> {
        return combine(
            delegate.observeThread(userId, otherUserId),
            outbox.entries,
            auth.session
        ) { remote, queued, session ->
            val accountId = client.auth.currentUserOrNull()?.id

            if (
                session?.user?.userId != userId ||
                accountId == null
            ) {
                emptyList()
            } else {
                val pending = queued.filter {
                    it.belongsTo(accountId, userId) &&
                            it.receiverId == otherUserId
                }.map {
                    it.toMessage()
                }

                (remote + pending).sortedWith(
                    compareBy<Message> {
                        it.timestamp
                    }.thenBy {
                        it.messageId
                    }
                )
            }
        }
    }

    override fun observeConversations(
        userId: Int
    ): Flow<List<Conversation>> {
        return combine(
            delegate.observeConversations(userId),
            users.observeUsers(),
            outbox.entries,
            auth.session
        ) { remote, directory, queued, session ->
            val accountId = client.auth.currentUserOrNull()?.id

            if (
                session?.user?.userId != userId ||
                accountId == null
            ) {
                emptyList()
            } else {
                val conversations = remote.associateBy {
                    it.participant.userId
                }.toMutableMap()

                val usersById = directory.associateBy {
                    it.userId
                }

                queued.filter {
                    it.belongsTo(accountId, userId)
                }.groupBy {
                    it.receiverId
                }.forEach { (receiverId, messages) ->
                    val participant =
                        usersById[receiverId] ?: return@forEach

                    val latest = messages.maxBy {
                        it.queuedAt
                    }.toMessage()

                    val existing = conversations[receiverId]

                    if (
                        existing == null ||
                        latest.timestamp >=
                        existing.lastMessage.timestamp
                    ) {
                        conversations[receiverId] = Conversation(
                            participant = participant,
                            lastMessage = latest,
                            unreadCount = existing?.unreadCount ?: 0
                        )
                    }
                }

                conversations.values.sortedByDescending {
                    it.lastMessage.timestamp
                }
            }
        }
    }

    override suspend fun sendMessage(
        senderId: Int,
        receiverId: Int,
        content: String
    ): Message {
        val session = auth.session.value
            ?: throw BackendSessionRequiredException()

        require(session.user.userId == senderId)

        require(receiverId > 0)
        require(receiverId != senderId)

        val trimmed = content.trim()

        require(trimmed.length in 1..5000) {
            "Messages must contain between 1 and 5000 characters."
        }

        val operation = gateway.prepare(
            BackendAction.MESSAGE_SEND,
            buildJsonObject {
                put("receiver_id", receiverId)
                put("content", trimmed)
            }
        )

        val entry = outbox.enqueue(
            senderId = senderId,
            receiverId = receiverId,
            operation = operation
        )

        return entry.toMessage()
    }

    override fun observeUnreadCount(
        userId: Int
    ): Flow<Int> {
        return delegate.observeUnreadCount(userId)
    }

    override suspend fun markThreadRead(
        userId: Int,
        otherUserId: Int
    ) {
        if (!sync.status.value.isOnline) {
            return
        }

        try {
            delegate.markThreadRead(userId, otherUserId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // A failed read receipt must not close the conversation.
            // Reopening the conversation can retry it.
        }
    }
}