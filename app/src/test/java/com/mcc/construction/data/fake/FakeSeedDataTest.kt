package com.mcc.construction.data.fake

import com.mcc.construction.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the seed data holds together the way the ERD says it must: every
 * foreign key points at a row that exists, and every primary key is unique.
 * A broken link here would show up as a blank name or a missing project
 * somewhere in the UI.
 */
class FakeSeedDataTest {

    private val seed = FakeSeedData(fixedClock)

    private val userIds = seed.users.map { it.userId }.toSet()
    private val projectIds = seed.projects.map { it.projectId }.toSet()
    private val taskIds = seed.tasks.map { it.taskId }.toSet()
    private val supplierIds = seed.suppliers.map { it.supplierId }.toSet()
    private val inspectionIds = seed.inspections.map { it.inspectionId }.toSet()

    @Test
    fun primaryKeysAreUnique() {
        assertUnique(seed.users.map { it.userId }, "USERS")
        assertUnique(seed.projects.map { it.projectId }, "PROJECTS")
        assertUnique(seed.tasks.map { it.taskId }, "TASKS")
        assertUnique(seed.assignments.map { it.assignmentId }, "USER_TASKS")
        assertUnique(seed.attendance.map { it.attendanceId }, "ATTENDANCE")
        assertUnique(seed.dailyReports.map { it.reportId }, "DAILY_REPORTS")
        assertUnique(seed.suppliers.map { it.supplierId }, "SUPPLIERS")
        assertUnique(seed.inventory.map { it.materialId }, "INVENTORY")
        assertUnique(seed.documents.map { it.documentId }, "DOCUMENTS")
        assertUnique(seed.inspections.map { it.inspectionId }, "SAFETY_INSPECTIONS")
        assertUnique(seed.incidents.map { it.incidentId }, "INCIDENT_REPORTS")
        assertUnique(seed.notifications.map { it.notificationId }, "NOTIFICATIONS")
        assertUnique(seed.messages.map { it.messageId }, "MESSAGES")
        assertUnique(seed.auditLog.map { it.logId }, "AUDIT_LOGS")
        assertUnique(seed.syncHistory.map { it.syncId }, "OFFLINE_SYNC")
    }

    @Test
    fun emailAddressesAreUnique() {
        assertUnique(seed.users.map { it.email.lowercase() }, "USERS.Email")
    }

    @Test
    fun foreignKeysResolve() {
        seed.projects.forEach { assertTrue("project manager ${it.managerId}", it.managerId in userIds) }
        seed.tasks.forEach { assertTrue("task project ${it.projectId}", it.projectId in projectIds) }
        seed.assignments.forEach {
            assertTrue("assignment user ${it.userId}", it.userId in userIds)
            assertTrue("assignment task ${it.taskId}", it.taskId in taskIds)
        }
        seed.attendance.forEach { assertTrue("attendance user ${it.userId}", it.userId in userIds) }
        seed.dailyReports.forEach {
            assertTrue("report project ${it.projectId}", it.projectId in projectIds)
            assertTrue("report user ${it.userId}", it.userId in userIds)
        }
        seed.inventory.forEach {
            assertTrue("inventory project ${it.projectId}", it.projectId in projectIds)
            assertTrue("inventory supplier ${it.supplierId}", it.supplierId in supplierIds)
        }
        seed.documents.forEach { assertTrue("document project ${it.projectId}", it.projectId in projectIds) }
        seed.inspections.forEach {
            assertTrue("inspection project ${it.projectId}", it.projectId in projectIds)
            assertTrue("inspection inspector ${it.inspectorId}", it.inspectorId in userIds)
        }
        seed.incidents.forEach {
            assertTrue("incident inspection ${it.inspectionId}", it.inspectionId in inspectionIds)
        }
        seed.notifications.forEach { assertTrue("notification user ${it.userId}", it.userId in userIds) }
        seed.messages.forEach {
            assertTrue("message sender ${it.senderId}", it.senderId in userIds)
            assertTrue("message receiver ${it.receiverId}", it.receiverId in userIds)
        }
        seed.auditLog.forEach { assertTrue("audit user ${it.userId}", it.userId in userIds) }
        seed.syncHistory.forEach { assertTrue("sync user ${it.userId}", it.userId in userIds) }
    }

    @Test
    fun projectManagersHoldTheManagerRole() {
        val managers = seed.users.filter { it.role == UserRole.MANAGER }.map { it.userId }.toSet()
        seed.projects.forEach { assertTrue(it.projectName, it.managerId in managers) }
    }

    @Test
    fun demoAccountsExistWithTheRightRoles() {
        fun roleOf(email: String) = seed.users.single { it.email == email }.role
        assertEquals(UserRole.WORKER, roleOf(FakeSeedData.DEMO_WORKER_EMAIL))
        assertEquals(UserRole.MANAGER, roleOf(FakeSeedData.DEMO_MANAGER_EMAIL))
        assertEquals(UserRole.ADMINISTRATOR, roleOf(FakeSeedData.DEMO_ADMIN_EMAIL))
    }

    @Test
    fun nothingIsDatedInTheFuture() {
        val now = FIXED_NOW
        seed.notifications.forEach { assertTrue("notification ${it.notificationId}", !it.sentDate.isAfter(now)) }
        seed.messages.forEach { assertTrue("message ${it.messageId}", !it.timestamp.isAfter(now)) }
        seed.attendance.forEach { assertTrue("attendance ${it.attendanceId}", !it.clockIn.isAfter(now)) }
        seed.pendingChanges.forEach { assertTrue("pending ${it.changeId}", !it.queuedAt.isAfter(now)) }
    }

    @Test
    fun demoWorkerStartsTheDayClockedOut() {
        val worker = seed.users.single { it.email == FakeSeedData.DEMO_WORKER_EMAIL }
        assertTrue(seed.attendance.none { it.userId == worker.userId && it.isOpen })
    }

    private fun <T> assertUnique(values: List<T>, table: String) {
        assertEquals("duplicate key in $table", values.size, values.toSet().size)
    }
}
