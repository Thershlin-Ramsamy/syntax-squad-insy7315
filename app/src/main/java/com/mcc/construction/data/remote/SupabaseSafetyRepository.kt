package com.mcc.construction.data.remote

import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.SafetyChecklistItem
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.data.remote.dto.IncidentRow
import com.mcc.construction.data.remote.dto.SafetyChecklistRow
import com.mcc.construction.data.remote.dto.SafetyInspectionRow
import com.mcc.construction.data.repository.IncidentDraft
import com.mcc.construction.data.repository.SafetyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseSafetyRepository(
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport,
    private val storage: BackendStorageService
) : SafetyRepository {

    override fun observeChecklistItems():
            Flow<List<SafetyChecklistItem>> {
        return reads.observeRows<SafetyChecklistRow>(
            table = "safety_checklist_items",
            primaryKey = "item_id"
        ).map { rows ->
            rows.filter {
                it.isActive
            }.sortedWith(
                compareBy<SafetyChecklistRow> {
                    it.sortOrder
                }.thenBy {
                    it.itemId
                }
            ).map { row ->
                SafetyChecklistItem(
                    itemId = row.itemId,
                    title = row.title,
                    detail = row.detail
                )
            }
        }
    }

    override fun observeInspections():
            Flow<List<SafetyInspection>> {
        return reads.observeRows<SafetyInspectionRow>(
            table = "safety_inspections",
            primaryKey = "inspection_id"
        ).map { rows ->
            rows.map { row ->
                mapInspection(row)
            }.sortedWith(
                compareByDescending<SafetyInspection> {
                    it.date
                }.thenByDescending {
                    it.inspectionId
                }
            )
        }
    }

    override fun observeInspectionsForProject(
        projectId: Int
    ): Flow<List<SafetyInspection>> {
        return observeInspections().map { inspections ->
            inspections.filter {
                it.projectId == projectId
            }
        }
    }

    override fun observeInspection(
        inspectionId: Int
    ): Flow<SafetyInspection?> {
        return observeInspections().map { inspections ->
            inspections.firstOrNull {
                it.inspectionId == inspectionId
            }
        }
    }

    override fun observeInspectionsBy(
        inspectorId: Int
    ): Flow<List<SafetyInspection>> {
        return observeInspections().map { inspections ->
            inspections.filter {
                it.inspectorId == inspectorId
            }
        }
    }

    override fun observeIncidents():
            Flow<List<IncidentReport>> {
        return reads.observeRows<IncidentRow>(
            table = "incident_reports",
            primaryKey = "incident_id"
        ).map { rows ->
            rows.sortedByDescending {
                it.dateReported
            }.map { row ->
                mapIncident(row)
            }
        }
    }

    override fun observeIncidentsForInspection(
        inspectionId: Int
    ): Flow<List<IncidentReport>> {
        return observeIncidents().map { incidents ->
            incidents.filter {
                it.inspectionId == inspectionId
            }
        }
    }

    override suspend fun submitInspection(
        projectId: Int,
        inspectorId: Int,
        passed: Boolean,
        comments: String
    ): SafetyInspection {
        require(projectId > 0)
        writes.requireCurrentUser(inspectorId)

        val row = writes.executeAs<SafetyInspectionRow>(
            action = BackendAction.INSPECTION_SUBMIT,
            payload = buildJsonObject {
                put("project_id", projectId)
                put("passed", passed)
                put("comments", comments.trim())
            }
        )

        return mapInspection(row)
    }

    override suspend fun reportIncident(
        draft: IncidentDraft
    ): IncidentReport {
        writes.requireCurrentUser(draft.reportedBy)

        require(draft.projectId > 0)
        require(draft.description.isNotBlank()) {
            "Describe the incident."
        }

        val paths = draft.photoUris.map { uri ->
            writes.requireCurrentUser(draft.reportedBy)

            storage.uploadPhoto(
                projectId = draft.projectId,
                uri = uri
            ).storagePath
        }

        writes.requireCurrentUser(draft.reportedBy)

        val row = writes.executeAs<IncidentRow>(
            action = BackendAction.INCIDENT_SUBMIT,
            payload = buildJsonObject {
                put("project_id", draft.projectId)
                put("severity_level", draft.severity.level)
                put("description", draft.description.trim())

                draft.inspectionId?.let {
                    put("inspection_id", it)
                }

                put(
                    "photo_paths",
                    JsonArray(paths.map { JsonPrimitive(it) })
                )
            }
        )

        return mapIncident(row)
    }

    private fun mapInspection(
        row: SafetyInspectionRow
    ): SafetyInspection {
        return SafetyInspection(
            inspectionId = row.inspectionId,
            projectId = row.projectId,
            inspectorId = row.inspectorId,
            date = BackendTime.date(row.date),
            comments = row.comments,
            passed = row.passed
        )
    }

    private suspend fun mapIncident(
        row: IncidentRow
    ): IncidentReport {
        return IncidentReport(
            incidentId = row.incidentId,
            inspectionId = row.inspectionId,
            severityLevel = row.severityLevel,
            description = row.description,
            dateReported = BackendTime.timestamp(row.dateReported),
            photoUris = storage.photoUrls(row.photoPaths)
        )
    }
}