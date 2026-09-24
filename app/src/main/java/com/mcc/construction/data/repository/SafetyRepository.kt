package com.mcc.construction.data.repository

import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.IncidentSeverity
import com.mcc.construction.data.model.SafetyChecklistItem
import com.mcc.construction.data.model.SafetyInspection
import kotlinx.coroutines.flow.Flow

/**
 * What a worker fills in to report an incident.
 *
 * [inspectionId] links the incident to the checklist it came from. The ERD
 * requires every incident to belong to an inspection, so when it is null —
 * an incident reported on its own — the repository records a failed
 * inspection for [projectId] to hold it.
 */
data class IncidentDraft(
    val projectId: Int,
    val reportedBy: Int,
    val severity: IncidentSeverity,
    val description: String,
    val photoUris: List<String> = emptyList(),
    val inspectionId: Int? = null
)

/** Safety checklists, inspections and incident reports. */
interface SafetyRepository {

    /** The items a worker ticks off on the daily site safety checklist. */
    fun observeChecklistItems(): Flow<List<SafetyChecklistItem>>

    /** Every inspection, newest first. */
    fun observeInspections(): Flow<List<SafetyInspection>>

    fun observeInspectionsForProject(projectId: Int): Flow<List<SafetyInspection>>

    fun observeInspection(inspectionId: Int): Flow<SafetyInspection?>

    /** Inspections [inspectorId] completed, newest first. */
    fun observeInspectionsBy(inspectorId: Int): Flow<List<SafetyInspection>>

    /** Every incident, newest first. */
    fun observeIncidents(): Flow<List<IncidentReport>>

    fun observeIncidentsForInspection(inspectionId: Int): Flow<List<IncidentReport>>

    /** Records a completed checklist. Queued for upload if the device is offline. */
    suspend fun submitInspection(
        projectId: Int,
        inspectorId: Int,
        passed: Boolean,
        comments: String
    ): SafetyInspection

    /** Mirrors the class diagram's `IncidentReport.fileReport()`. Queued if the device is offline. */
    suspend fun reportIncident(draft: IncidentDraft): IncidentReport
}
