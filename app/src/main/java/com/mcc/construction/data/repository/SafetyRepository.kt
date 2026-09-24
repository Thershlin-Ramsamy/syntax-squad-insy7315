package com.mcc.construction.data.repository

import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.IncidentSeverity
import com.mcc.construction.data.model.SafetyChecklistItem
import com.mcc.construction.data.model.SafetyInspection
import kotlinx.coroutines.flow.Flow

/** Safety checklists, inspections and incident reports. */
interface SafetyRepository {

    /** The items a worker ticks off on the daily site safety checklist. */
    fun observeChecklistItems(): Flow<List<SafetyChecklistItem>>

    /** Every inspection, newest first. */
    fun observeInspections(): Flow<List<SafetyInspection>>

    fun observeInspectionsForProject(projectId: Int): Flow<List<SafetyInspection>>

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

    /** Queued for upload if the device is offline. */
    suspend fun reportIncident(
        inspectionId: Int,
        severity: IncidentSeverity,
        description: String
    ): IncidentReport
}
