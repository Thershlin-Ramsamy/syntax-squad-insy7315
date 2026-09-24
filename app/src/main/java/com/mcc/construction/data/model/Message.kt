package com.mcc.construction.data.model

import java.time.LocalDateTime

/** A row of the MESSAGES table: one direct message between two users. */
data class Message(
    val messageId: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: LocalDateTime
)

/**
 * A chat-list entry, derived from MESSAGES rather than stored: every message
 * between the signed-in user and [participant], summarised by the latest one.
 */
data class Conversation(
    val participant: User,
    val lastMessage: Message
)
