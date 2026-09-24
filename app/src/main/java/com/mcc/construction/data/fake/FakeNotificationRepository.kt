package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Notification
import com.mcc.construction.data.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeNotificationRepository(seed: FakeSeedData) : NotificationRepository {

    private val notifications = MutableStateFlow(seed.notifications)

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
}
