package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Notification
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.repository.NotificationRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeNotificationRepository(
    seed: FakeSeedData,
    private val clock: Clock
) : NotificationRepository {

    private val notifications = MutableStateFlow(seed.notifications)
    private val nextId = AtomicInteger(seed.notifications.maxOf { it.notificationId } + 1)

    override fun observeNotifications(userId: Int): Flow<List<Notification>> =
        notifications.map { list ->
            list.filter { it.userId == userId }.sortedByDescending { it.sentDate }
        }

    override fun observeUnreadCount(userId: Int): Flow<Int> =
        notifications
            .map { list -> list.count { it.userId == userId && !it.isRead } }
            .distinctUntilChanged()

    override suspend fun markAsRead(notificationId: Int) {
        notifications.update { list ->
            list.map { if (it.notificationId == notificationId) it.copy(isRead = true) else it }
        }
    }

    override suspend fun markAllAsRead(userId: Int) {
        notifications.update { list ->
            list.map { if (it.userId == userId) it.copy(isRead = true) else it }
        }
    }

    /** Delivers a new unread notification, as the server would. */
    internal fun add(userId: Int, type: NotificationType, message: String): Notification {
        val notification = Notification(
            notificationId = nextId.getAndIncrement(),
            userId = userId,
            type = type,
            messageContent = message,
            sentDate = LocalDateTime.now(clock),
            isRead = false
        )
        notifications.update { it + notification }
        return notification
    }
}
