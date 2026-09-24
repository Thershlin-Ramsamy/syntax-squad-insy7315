package com.mcc.construction.data.model

import java.time.LocalDate

/**
 * Review state of a daily report. Not in the ERD yet — it backs the class
 * diagram's `Manager.approveReport()` and is listed in docs/ARCHITECTURE.md
 * under "Fields beyond the ERD".
 */
enum class ReportStatus { SUBMITTED, APPROVED }

/** A row of the DAILY_REPORTS table. */
data class DailyReport(
    val reportId: Int,
    val projectId: Int,
    val userId: Int,
    val date: LocalDate,
    val weather: String,
    val workCompleted: String,
    val problemsEncountered: String,
    val status: ReportStatus
)
