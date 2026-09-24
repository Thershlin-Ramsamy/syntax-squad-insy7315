package com.mcc.construction.data.repository

import com.mcc.construction.data.model.DailyReport
import kotlinx.coroutines.flow.Flow

/** What a worker fills in to submit a daily report. */
data class DailyReportDraft(
    val projectId: Int,
    val userId: Int,
    val weather: String,
    val workCompleted: String,
    val problemsEncountered: String,
    val photoUris: List<String> = emptyList()
)

interface DailyReportRepository {

    /** Every report, newest first — the manager's reports history. */
    fun observeReports(): Flow<List<DailyReport>>

    fun observeReportsBy(userId: Int): Flow<List<DailyReport>>

    fun observeReport(reportId: Int): Flow<DailyReport?>

    /** Saves the report for today. Queued for upload if the device is offline. */
    suspend fun submitReport(draft: DailyReportDraft): DailyReport

    /** Mirrors the class diagram's `Manager.approveReport()`. */
    suspend fun approveReport(reportId: Int)
}
