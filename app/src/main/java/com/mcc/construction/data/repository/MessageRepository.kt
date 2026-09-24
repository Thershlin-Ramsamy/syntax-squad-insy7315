package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Conversation
import com.mcc.construction.data.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {

    /** One entry per person [userId] has messaged, most recent first. */
    fun observeConversations(userId: Int): Flow<List<Conversation>>

    /** Every message between the two users, oldest first — chat order. */
    fun observeThread(userId: Int, otherUserId: Int): Flow<List<Message>>

    /** Queued for upload if the device is offline. */
    suspend fun sendMessage(senderId: Int, receiverId: Int, content: String): Message

    /** Messages sent to [userId] that they have not opened yet, across all conversations. */
    fun observeUnreadCount(userId: Int): Flow<Int>

    /** Marks every message [otherUserId] sent to [userId] as read. */
    suspend fun markThreadRead(userId: Int, otherUserId: Int)
}
