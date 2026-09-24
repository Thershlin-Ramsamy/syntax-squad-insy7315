package com.mcc.construction.data.fake

import com.mcc.construction.data.model.IncidentSeverity
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.repository.IncidentDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Repository behaviour the worker screens rely on. */
class FakeWorkerRepositoriesTest {

    private val container = testContainer()
    private val workerId = 5

    @Test
    fun proofPhotosAreStoredPerTaskAndCanBeRemoved() = runTest {
        val tasks = container.taskRepository
        val photo = tasks.addProofPhoto(taskId = 101, userId = workerId, uri = "content://photo/1")
        tasks.addProofPhoto(taskId = 102, userId = workerId, uri = "content://photo/2")

        assertEquals(listOf(photo), tasks.observeProofPhotos(101).first())

        tasks.removeProofPhoto(photo.photoId)
        assertTrue(tasks.observeProofPhotos(101).first().isEmpty())
    }

    @Test
    fun incidentFromAChecklistKeepsItsInspection() = runTest {
        val safety = container.safetyRepository
        val inspection = safety.submitInspection(1, workerId, passed = false, comments = "Scaffold tag missing")

        val incident = safety.reportIncident(
            IncidentDraft(1, workerId, IncidentSeverity.MODERATE, "Tag missing on bay 2", inspectionId = inspection.inspectionId)
        )

        assertEquals(inspection.inspectionId, incident.inspectionId)
        assertEquals(3, incident.severityLevel)
    }

    @Test
    fun incidentOnItsOwnGetsAFailedInspection() = runTest {
        val safety = container.safetyRepository
        val before = safety.observeInspections().first().size

        val incident = safety.reportIncident(
            IncidentDraft(1, workerId, IncidentSeverity.CRITICAL, "Trench wall collapsed", listOf("content://p"))
        )

        val inspections = safety.observeInspections().first()
        assertEquals(before + 1, inspections.size)
        val created = inspections.single { it.inspectionId == incident.inspectionId }
        assertFalse(created.passed)
        assertEquals(listOf("content://p"), incident.photoUris)
    }

    @Test
    fun incidentReportedOfflineIsQueued() = runTest {
        container.syncRepository.setOfflineMode(true)

        container.safetyRepository.reportIncident(
            IncidentDraft(1, workerId, IncidentSeverity.MINOR, "Cut finger on rebar tie")
        )

        assertEquals(
            PendingChangeType.INCIDENT_REPORT,
            container.syncRepository.status.value.pendingChanges.last().type
        )
    }

    @Test
    fun severityCoversOneToFive() {
        assertEquals((1..5).toList(), IncidentSeverity.entries.map { it.level })
        assertEquals(IncidentSeverity.CRITICAL, IncidentSeverity.fromLevel(9))
    }

    @Test
    fun openingAThreadMarksItRead() = runTest {
        val messages = container.messageRepository
        val johan = 3
        val before = messages.observeUnreadCount(workerId).first()
        val johanThread = messages.observeConversations(workerId).first().single { it.participant.userId == johan }
        assertTrue(johanThread.unreadCount > 0)

        messages.markThreadRead(workerId, johan)

        val after = messages.observeConversations(workerId).first().single { it.participant.userId == johan }
        assertEquals(0, after.unreadCount)
        assertEquals(before - johanThread.unreadCount, messages.observeUnreadCount(workerId).first())
    }

    @Test
    fun sentMessagesAppearInTheThreadAndAsUnreadForTheReceiver() = runTest {
        val messages = container.messageRepository
        val johan = 3
        val unreadForJohan = messages.observeUnreadCount(johan).first()

        messages.sendMessage(workerId, johan, "Rebar spacing checked")

        assertEquals("Rebar spacing checked", messages.observeThread(workerId, johan).first().last().content)
        assertEquals(unreadForJohan + 1, messages.observeUnreadCount(johan).first())
    }
}
