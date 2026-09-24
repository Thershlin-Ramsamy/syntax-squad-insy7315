package com.mcc.construction.data.model

import java.math.BigDecimal
import java.time.LocalDate

enum class ProjectStatus { PLANNING, ACTIVE, ON_HOLD, COMPLETED }

/**
 * A row of the PROJECTS table.
 *
 * [budget] is a [BigDecimal] to match `decimal(12,2)` — rand amounts are never
 * held in floating point. [progressPercent] matches `Progress decimal(5,2)`,
 * so it runs from 0 to 100; [progressFraction] is the 0–1 form progress bars
 * expect.
 */
data class Project(
    val projectId: Int,
    val managerId: Int,
    val projectName: String,
    val location: String,
    val budget: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: ProjectStatus,
    val progressPercent: Double
) {
    val progressFraction: Float
        get() = (progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
}
