package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Notification
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.remote.dto.NotificationRow
import com.mcc.construction.data.repository.NotificationRepository
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseNotificationRepository(
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport
) : NotificationRepository {

    override fun observeNotifications(
        userId: Int
    ): Flow<List<Notification>> {
        return reads.observeRows<NotificationRow>(
            table = "notifications",
            primaryKey = "notification_id"
        ).map { rows ->
            rows.filter {
                it.userId == userId
            }.map { row ->
                mapNotification(row)
            }.sortedWith(
                compareByDescending<Notification> {
                    it.sentDate
                }.thenByDescending {
                    it.notificationId
                }
            )
        }
    }

    override fun observeUnreadCount(
        userId: Int
    ): Flow<Int> {
        return observeNotifications(userId).map { notifications ->
            notifications.count {
                !it.isRead
            }
        }
    }

    override suspend fun markAsRead(
        notificationId: Int
    ) {
        require(notificationId > 0) {
            "A positive notification ID is required."
        }

        writes.execute(
            action = BackendAction.NOTIFICATION_READ,
            payload = buildJsonObject {
                put("notification_id", notificationId)
            }
        )
    }

    override suspend fun markAllAsRead(
        userId: Int
    ) {
        writes.requireCurrentUser(userId)

        writes.execute(
            action = BackendAction.NOTIFICATIONS_READ,
            payload = buildJsonObject {}
        )
    }

    private fun mapNotification(
        row: NotificationRow
    ): Notification {
        return Notification(
            notificationId = row.notificationId,
            userId = row.userId,
            type = NotificationType.valueOf(row.type),
            messageContent = row.messageContent,
            sentDate = OffsetDateTime.parse(row.sentDate)
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