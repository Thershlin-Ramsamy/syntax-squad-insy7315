package com.mcc.construction.data.model

import java.time.LocalDateTime

/**
 * A row of the MESSAGES table: one direct message between two users.
 *
 * [isRead] — whether the receiver has opened it — is not in the ERD yet; see
 * "Fields beyond the ERD" in docs/ARCHITECTURE.md.
 */
data class Message(
    val messageId: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: LocalDateTime,
    val isRead: Boolean = true
)

/**
 * A chat-list entry, derived from MESSAGES rather than stored: every message
 * between the signed-in user and [participant], summarised by the latest one.
 */
data class Conversation(
    val participant: User,
    val lastMessage: Message,
    /** Messages from [participant] the signed-in user has not opened yet. */
    val unreadCount: Int = 0
)
