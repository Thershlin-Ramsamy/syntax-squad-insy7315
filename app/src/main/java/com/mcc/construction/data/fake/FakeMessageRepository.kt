package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Conversation
import com.mcc.construction.data.model.Message
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.repository.MessageRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeMessageRepository(
    seed: FakeSeedData,
    private val users: FakeUserRepository,
    private val sync: FakeSyncRepository,
    private val clock: Clock
) : MessageRepository {

    private val messages = MutableStateFlow(seed.messages)
    private val nextId = AtomicInteger(seed.messages.maxOf { it.messageId } + 1)

    override fun observeConversations(userId: Int): Flow<List<Conversation>> =
        combine(messages, users.observeUsers()) { messageList, userList ->
            messageList
                .filter { it.senderId == userId || it.receiverId == userId }
                .groupBy { if (it.senderId == userId) it.receiverId else it.senderId }
                .mapNotNull { (otherUserId, thread) ->
                    val participant = userList.firstOrNull { it.userId == otherUserId }
                        ?: return@mapNotNull null
                    Conversation(
                        participant = participant,
                        lastMessage = thread.maxBy { it.timestamp },
                        unreadCount = thread.count { it.receiverId == userId && !it.isRead }
                    )
                }
                .sortedByDescending { it.lastMessage.timestamp }
        }

    override fun observeThread(userId: Int, otherUserId: Int): Flow<List<Message>> =
        messages.map { list ->
            list.filter {
                (it.senderId == userId && it.receiverId == otherUserId) ||
                    (it.senderId == otherUserId && it.receiverId == userId)
            }.sortedBy { it.timestamp }
        }

    override suspend fun sendMessage(senderId: Int, receiverId: Int, content: String): Message {
        val message = Message(
            messageId = nextId.getAndIncrement(),
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = LocalDateTime.now(clock),
            isRead = false
        )
        messages.update { it + message }
        sync.recordChange(
            PendingChangeType.MESSAGE,
            users.findUser(receiverId)?.fullName.orEmpty()
        )
        return message
    }

    override fun observeUnreadCount(userId: Int): Flow<Int> =
        messages.map { list -> list.count { it.receiverId == userId && !it.isRead } }
            .distinctUntilChanged()

    override suspend fun markThreadRead(userId: Int, otherUserId: Int) {
        messages.update { list ->
            list.map {
                if (it.receiverId == userId && it.senderId == otherUserId && !it.isRead) {
                    it.copy(isRead = true)
                } else {
                    it
                }
            }
        }
    }
}
