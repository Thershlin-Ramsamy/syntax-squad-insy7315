package com.mcc.construction.data.fake

import com.mcc.construction.data.model.NotificationType

/**
 * Sends the notifications the server will send once there is one: when a task
 * is assigned, when someone moves a task on, when a report is submitted or
 * approved, when a safety problem is reported, and when stock runs low.
 *
 * Messages are plain data composed here, as they would arrive from the API.
 * Nobody is notified about their own actions.
 */
class FakeNotifier(
    private val notifications: FakeNotificationRepository,
    private val users: FakeUserRepository,
    private val projects: FakeProjectRepository,
    private val currentUserId: () -> Int?
) {

    /** The signed-in person's name, for "X assigned you …" messages. */
    fun actorName(): String = currentUserId()?.let { users.findUser(it)?.fullName } ?: "The site office"

    fun nameOf(userId: Int): String = users.findUser(userId)?.fullName.orEmpty()

    fun projectName(projectId: Int): String = projects.findProject(projectId)?.projectName.orEmpty()

    /** Notifies [userId], unless they are the one who caused it. */
    fun notify(userId: Int, type: NotificationType, message: String) {
        if (userId == currentUserId()) return
        notifications.add(userId, type, message)
    }

    /** Notifies the manager of [projectId], unless they caused it. */
    fun notifyProjectManager(projectId: Int, type: NotificationType, message: String) {
        val managerId = projects.findProject(projectId)?.managerId ?: return
        notify(managerId, type, message)
    }
}
