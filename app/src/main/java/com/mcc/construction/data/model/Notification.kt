package com.mcc.construction.data.model

import java.time.LocalDateTime

/**
 * What a notification is about. Not in the ERD yet — it picks the icon and the
 * screen a notification opens, and is listed in docs/ARCHITECTURE.md under
 * "Fields beyond the ERD".
 */
enum class NotificationType { TASK, SAFETY, MESSAGE, INVENTORY, REPORT, SYSTEM }

/** A row of the NOTIFICATIONS table. */
data class Notification(
    val notificationId: Int,
    val userId: Int,
    val type: NotificationType,
    val messageContent: String,
    val sentDate: LocalDateTime,
    val isRead: Boolean
)
