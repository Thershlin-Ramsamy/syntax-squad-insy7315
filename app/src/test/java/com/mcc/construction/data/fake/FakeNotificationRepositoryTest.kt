package com.mcc.construction.data.fake

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNotificationRepositoryTest {

    private val notifications = testContainer().notificationRepository
    private val workerId = 5
    private val managerId = 2

    @Test
    fun listIsNewestFirstAndOnlyForThatUser() = runTest {
        val list = notifications.observeNotifications(workerId).first()

        assertTrue(list.all { it.userId == workerId })
        assertEquals(list.sortedByDescending { it.sentDate }, list)
    }

    @Test
    fun markAsReadLowersTheUnreadCount() = runTest {
        val before = notifications.observeUnreadCount(workerId).first()
        val unread = notifications.observeNotifications(workerId).first().first { !it.isRead }

        notifications.markAsRead(unread.notificationId)

        assertEquals(before - 1, notifications.observeUnreadCount(workerId).first())
    }

    @Test
    fun markAllAsReadOnlyTouchesThatUser() = runTest {
        val managerUnread = notifications.observeUnreadCount(managerId).first()

        notifications.markAllAsRead(workerId)

        assertEquals(0, notifications.observeUnreadCount(workerId).first())
        assertEquals(managerUnread, notifications.observeUnreadCount(managerId).first())
    }
}
