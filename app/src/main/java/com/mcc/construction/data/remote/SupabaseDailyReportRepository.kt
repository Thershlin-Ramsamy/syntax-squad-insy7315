package com.mcc.construction.data.remote

import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.Weather
import com.mcc.construction.data.remote.dto.DailyReportRow
import com.mcc.construction.data.repository.DailyReportDraft
import com.mcc.construction.data.repository.DailyReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseDailyReportRepository(
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport,
    private val storage: BackendStorageService
) : DailyReportRepository {

    override fun observeReports(): Flow<List<DailyReport>> {
        return reads.mapRows(
            "daily_reports",
            reads.observeRows<DailyReportRow>(
                table = "daily_reports",
                primaryKey = "report_id"
            )
        ) { rows ->
            rows.sortedWith(
                compareByDescending<DailyReportRow> {
                    it.date
                }.thenByDescending {
                    it.reportId
                }
            ).map {
                mapReport(it)
            }
        }
    }

    override fun observeReportsBy(
        userId: Int
    ): Flow<List<DailyReport>> {
        return observeReports().map { reports ->
            reports.filter { it.userId == userId }
        }
    }

    override fun observeReport(
        reportId: Int
    ): Flow<DailyReport?> {
        return observeReports().map { reports ->
            reports.firstOrNull { it.reportId == reportId }
        }
    }

    override suspend fun submitReport(
        draft: DailyReportDraft
    ): DailyReport {
        writes.requireCurrentUser(draft.userId)
        require(draft.projectId > 0)

        require(draft.workCompleted.isNotBlank()) {
            "Describe the work completed."
        }

        require(
            Weather.entries.any {
                it.storedValue == draft.weather
            }
        ) {
            "Choose a supported weather value."
        }

        val paths = draft.photoUris.map { uri ->
            writes.requireCurrentUser(draft.userId)

            storage.uploadPhoto(
                projectId = draft.projectId,
                uri = uri
            ).storagePath
        }

        writes.requireCurrentUser(draft.userId)

        val row = writes.executeAs<DailyReportRow>(
            action = BackendAction.REPORT_SUBMIT,
            payload = buildJsonObject {
                put("project_id", draft.projectId)
                put("weather", draft.weather)
                put("work_completed", draft.workCompleted.trim())
                put(
                    "problems_encountered",
                    draft.problemsEncountered.trim()
                )
                put(
                    "photo_paths",
                    JsonArray(
                        paths.map { JsonPrimitive(it) }
                    )
                )
            }
        )

        return mapReport(row)
    }

    override suspend fun approveReport(reportId: Int) {
        require(reportId > 0)

        writes.execute(
            action = BackendAction.REPORT_APPROVE,
            payload = buildJsonObject {
                put("report_id", reportId)
            }
        )
    }

    private suspend fun mapReport(row: DailyReportRow): DailyReport {
        return DailyReport(
            reportId = row.reportId,
            projectId = row.projectId,
            userId = row.userId,
            date = BackendTime.date(row.date),
            weather = row.weather,
            workCompleted = row.workCompleted,
            problemsEncountered = row.problemsEncountered,
            status = ReportStatus.valueOf(row.status),
            photoUris = storage.photoUrls(row.photoPaths)
        )
    }
}