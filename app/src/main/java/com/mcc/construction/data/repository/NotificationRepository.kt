package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    /** Notifications for [userId], newest first. */
    fun observeNotifications(userId: Int): Flow<List<Notification>>

    fun observeUnreadCount(userId: Int): Flow<Int>

    suspend fun markAsRead(notificationId: Int)

    suspend fun markAllAsRead(userId: Int)
}
