package com.mcc.construction.data.fake

import com.mcc.construction.data.model.DocumentCategory
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.SystemSettings
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.SignInResult
import com.mcc.construction.data.repository.TaskDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The manager → worker → manager loop the demo is built around. */
class FakeManagerWorkflowTest {

    private val workerId = 5
    private val managerId = 2
    private val adminId = 1

    private fun draft() = TaskDraft(
        projectId = 1,
        title = "Strip formwork on Block C",
        description = "Strip and stack the level 2 formwork.",
        deadline = FIXED_NOW.toLocalDate().plusDays(2),
        priority = TaskPriority.HIGH
    )

    @Test
    fun assignedTaskReachesTheWorkerWithANotification() = runTest {
        val c = testContainer()
        c.authRepository.signIn(FakeSeedData.DEMO_MANAGER_EMAIL, "pw")
        val unreadBefore = c.notificationRepository.observeUnreadCount(workerId).first()

        val task = c.taskRepository.createTask(draft())
        c.taskRepository.assignTask(task.taskId, workerId)

        assertEquals(TaskStatus.PENDING, task.status)
        assertTrue(c.taskRepository.observeTasksAssignedTo(workerId).first().any { it.taskId == task.taskId })
        assertEquals(unreadBefore + 1, c.notificationRepository.observeUnreadCount(workerId).first())
        val newest = c.notificationRepository.observeNotifications(workerId).first().first()
        assertEquals(NotificationType.TASK, newest.type)
        assertTrue(newest.messageContent.contains("Naledi Khumalo"))
        assertTrue(newest.messageContent.contains(task.title))
    }

    @Test
    fun assigningTheSamePersonTwiceDoesNotNotifyAgain() = runTest {
        val c = testContainer()
        c.authRepository.signIn(FakeSeedData.DEMO_MANAGER_EMAIL, "pw")
        val task = c.taskRepository.createTask(draft())

        c.taskRepository.assignTask(task.taskId, workerId)
        val afterFirst = c.notificationRepository.observeUnreadCount(workerId).first()
        c.taskRepository.assignTask(task.taskId, workerId)

        assertEquals(afterFirst, c.notificationRepository.observeUnreadCount(workerId).first())
        assertEquals(1, c.taskRepository.observeAssignments(task.taskId).first().size)
    }

    @Test
    fun workerProgressNotifiesTheProjectManager() = runTest {
        val c = testContainer()
        c.authRepository.signIn(FakeSeedData.DEMO_MANAGER_EMAIL, "pw")
        val task = c.taskRepository.createTask(draft())
        c.taskRepository.assignTask(task.taskId, workerId)
        c.authRepository.signOut()

        c.authRepository.signIn(FakeSeedData.DEMO_WORKER_EMAIL, "pw")
        val managerUnread = c.notificationRepository.observeUnreadCount(managerId).first()
        c.taskRepository.updateStatus(task.taskId, TaskStatus.IN_PROGRESS)

        assertEquals(TaskStatus.IN_PROGRESS, c.taskRepository.observeTask(task.taskId).first()?.status)
        assertEquals(managerUnread + 1, c.notificationRepository.observeUnreadCount(managerId).first())
        val newest = c.notificationRepository.observeNotifications(managerId).first().first()
        assertTrue(newest.messageContent.contains("Sipho Dlamini started"))
    }

    @Test
    fun managersAreNotNotifiedAboutTheirOwnChanges() = runTest {
        val c = testContainer()
        c.authRepository.signIn(FakeSeedData.DEMO_MANAGER_EMAIL, "pw")
        val before = c.notificationRepository.observeUnreadCount(managerId).first()

        val task = c.taskRepository.createTask(draft())
        c.taskRepository.updateStatus(task.taskId, TaskStatus.BLOCKED)

        assertEquals(before, c.notificationRepository.observeUnreadCount(managerId).first())
    }

    @Test
    fun approvingAReportNotifiesItsAuthor() = runTest {
        val c = testContainer()
        c.authRepository.signIn(FakeSeedData.DEMO_MANAGER_EMAIL, "pw")
        val report = c.dailyReportRepository.observeReports().first().first { it.status != ReportStatus.APPROVED }
        val before = c.notificationRepository.observeUnreadCount(report.userId).first()

        c.dailyReportRepository.approveReport(report.reportId)

        assertEquals(ReportStatus.APPROVED, c.dailyReportRepository.observeReport(report.reportId).first()?.status)
        assertEquals(before + 1, c.notificationRepository.observeUnreadCount(report.userId).first())
    }

    @Test
    fun stockFallingBelowMinimumAlertsTheManagerOnce() = runTest {
        val c = testContainer()
        c.authRepository.signIn(FakeSeedData.DEMO_WORKER_EMAIL, "pw")
        val before = c.notificationRepository.observeUnreadCount(managerId).first()

        // Cement on Riverside: 240 bags against a minimum of 100.
        c.inventoryRepository.adjustStock(1, -150)
        c.inventoryRepository.adjustStock(1, -10)

        assertEquals(before + 1, c.notificationRepository.observeUnreadCount(managerId).first())
    }

    @Test
    fun attendanceOnADayCoversEveryone() = runTest {
        val c = testContainer()
        val today = c.attendanceRepository.observeAttendanceOn(FIXED_NOW.toLocalDate()).first()

        assertTrue(today.isNotEmpty())
        assertTrue(today.all { it.date == FIXED_NOW.toLocalDate() })
        assertTrue(today.map { it.userId }.distinct().size > 1)
    }

    @Test
    fun uploadingTheSameFileAddsAVersion() = runTest {
        val c = testContainer()
        val existing = c.documentRepository.observeDocuments().first().first { it.documentId == 1 }

        val upload = c.documentRepository.uploadDocument(
            existing.projectId, existing.fileName, DocumentCategory.DRAWING
        )
        val fresh = c.documentRepository.uploadDocument(1, "Site-Plan.pdf", DocumentCategory.DRAWING)

        assertEquals(existing.version + 1, upload.version)
        assertEquals(1, fresh.version)
        assertEquals(fresh, c.documentRepository.observeDocuments().first().first())
    }

    @Test
    fun suspendedAccountsCannotSignInAndChangesAreAudited() = runTest {
        val c = testContainer()
        c.authRepository.signIn(FakeSeedData.DEMO_ADMIN_EMAIL, "pw")
        val auditBefore = c.auditLogRepository.observeAuditLog().first().size

        c.userRepository.setActive(workerId, false)
        c.userRepository.assignRole(7, UserRole.MANAGER)
        c.authRepository.signOut()

        assertEquals(SignInResult.AccountSuspended, c.authRepository.signIn(FakeSeedData.DEMO_WORKER_EMAIL, "pw"))
        val log = c.auditLogRepository.observeAuditLog().first()
        assertEquals(auditBefore + 2, log.size)
        assertTrue(log.take(2).all { it.userId == adminId })
    }

    @Test
    fun systemSettingChangesAreAudited() = runTest {
        val c = testContainer()
        c.authRepository.signIn(FakeSeedData.DEMO_ADMIN_EMAIL, "pw")
        val before = c.auditLogRepository.observeAuditLog().first().size

        c.systemSettingsRepository.update(
            SystemSettings(sessionTimeoutMinutes = 60, allowOfflineMode = false)
        )

        assertEquals(60, c.systemSettingsRepository.settings.value.sessionTimeoutMinutes)
        assertEquals(before + 2, c.auditLogRepository.observeAuditLog().first().size)
    }
}
