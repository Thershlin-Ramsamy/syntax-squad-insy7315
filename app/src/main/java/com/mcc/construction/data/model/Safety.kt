package com.mcc.construction.data.model

import java.time.LocalDate
import java.time.LocalDateTime

/** A row of the SAFETY_INSPECTIONS table. `PassFail` is modelled as [passed]. */
data class SafetyInspection(
    val inspectionId: Int,
    val projectId: Int,
    val inspectorId: Int,
    val date: LocalDate,
    val comments: String,
    val passed: Boolean
)

/**
 * The meaning of `INCIDENT_REPORTS.SeverityLevel`. The ERD stores a bare
 * integer; this enum fixes what each value means so every screen agrees.
 */
enum class IncidentSeverity(val level: Int) {
    NEAR_MISS(1),
    MINOR(2),
    SERIOUS(3);

    companion object {
        /** Unknown levels are treated as the most severe, never the least. */
        fun fromLevel(level: Int): IncidentSeverity =
            entries.firstOrNull { it.level == level } ?: SERIOUS
    }
}

/** A row of the INCIDENT_REPORTS table. Every incident hangs off an inspection. */
data class IncidentReport(
    val incidentId: Int,
    val inspectionId: Int,
    val severityLevel: Int,
    val description: String,
    val dateReported: LocalDateTime
) {
    val severity: IncidentSeverity get() = IncidentSeverity.fromLevel(severityLevel)
}

/**
 * One line of the daily site safety checklist. Not an ERD table: the checklist
 * is a template the worker completes, and the result is stored as a
 * [SafetyInspection]. Listed in docs/ARCHITECTURE.md under "Fields beyond the
 * ERD".
 */
data class SafetyChecklistItem(
    val itemId: Int,
    val title: String,
    val detail: String
)
