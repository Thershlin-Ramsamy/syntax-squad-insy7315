package com.mcc.construction.data.fake

import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.SafetyChecklistItem
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.data.repository.IncidentDraft
import com.mcc.construction.data.repository.SafetyRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeSafetyRepository(
    seed: FakeSeedData,
    private val projects: FakeProjectRepository,
    private val sync: FakeSyncRepository,
    private val clock: Clock
) : SafetyRepository {

    private val checklistItems = seed.checklistItems
    private val inspections = MutableStateFlow(seed.inspections)
    private val incidents = MutableStateFlow(seed.incidents)
    private val nextInspectionId = AtomicInteger(seed.inspections.maxOf { it.inspectionId } + 1)
    private val nextIncidentId = AtomicInteger(seed.incidents.maxOf { it.incidentId } + 1)

    override fun observeChecklistItems(): Flow<List<SafetyChecklistItem>> = flowOf(checklistItems)

    override fun observeInspections(): Flow<List<SafetyInspection>> =
        inspections.map { list -> list.sortedWith(NEWEST_FIRST) }

    override fun observeInspectionsForProject(projectId: Int): Flow<List<SafetyInspection>> =
        inspections.map { list ->
            list.filter { it.projectId == projectId }.sortedByDescending { it.date }
        }

    override fun observeInspection(inspectionId: Int): Flow<SafetyInspection?> =
        inspections.map { list -> list.firstOrNull { it.inspectionId == inspectionId } }

    override fun observeInspectionsBy(inspectorId: Int): Flow<List<SafetyInspection>> =
        inspections.map { list ->
            list.filter { it.inspectorId == inspectorId }.sortedWith(NEWEST_FIRST)
        }

    override fun observeIncidents(): Flow<List<IncidentReport>> =
        incidents.map { list -> list.sortedByDescending { it.dateReported } }

    override fun observeIncidentsForInspection(inspectionId: Int): Flow<List<IncidentReport>> =
        incidents.map { list ->
            list.filter { it.inspectionId == inspectionId }.sortedByDescending { it.dateReported }
        }

    override suspend fun submitInspection(
        projectId: Int,
        inspectorId: Int,
        passed: Boolean,
        comments: String
    ): SafetyInspection {
        val inspection = recordInspection(projectId, inspectorId, passed, comments)
        sync.recordChange(
            PendingChangeType.SAFETY_INSPECTION,
            projects.findProject(projectId)?.projectName.orEmpty()
        )
        return inspection
    }

    override suspend fun reportIncident(draft: IncidentDraft): IncidentReport {
        // The ERD needs an inspection behind every incident. One reported on
        // its own gets a failed inspection recorded for it, dated today.
        val inspectionId = draft.inspectionId ?: recordInspection(
            projectId = draft.projectId,
            inspectorId = draft.reportedBy,
            passed = false,
            comments = draft.description.take(SUMMARY_LENGTH)
        ).inspectionId

        val incident = IncidentReport(
            incidentId = nextIncidentId.getAndIncrement(),
            inspectionId = inspectionId,
            severityLevel = draft.severity.level,
            description = draft.description,
            dateReported = LocalDateTime.now(clock),
            photoUris = draft.photoUris
        )
        incidents.update { it + incident }
        sync.recordChange(PendingChangeType.INCIDENT_REPORT, draft.description.take(SUMMARY_LENGTH))
        return incident
    }

    private fun recordInspection(
        projectId: Int,
        inspectorId: Int,
        passed: Boolean,
        comments: String
    ): SafetyInspection {
        val inspection = SafetyInspection(
            inspectionId = nextInspectionId.getAndIncrement(),
            projectId = projectId,
            inspectorId = inspectorId,
            date = LocalDate.now(clock),
            comments = comments,
            passed = passed
        )
        inspections.update { it + inspection }
        return inspection
    }

    private companion object {
        const val SUMMARY_LENGTH = 60

        val NEWEST_FIRST = compareByDescending<SafetyInspection> { it.date }
            .thenByDescending { it.inspectionId }
    }
}
