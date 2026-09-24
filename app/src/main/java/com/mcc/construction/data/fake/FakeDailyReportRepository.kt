package com.mcc.construction.data.fake

import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.repository.DailyReportDraft
import com.mcc.construction.data.repository.DailyReportRepository
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeDailyReportRepository(
    seed: FakeSeedData,
    private val projects: FakeProjectRepository,
    private val sync: FakeSyncRepository,
    private val notifier: FakeNotifier,
    private val clock: Clock
) : DailyReportRepository {

    private val reports = MutableStateFlow(seed.dailyReports)
    private val nextId = AtomicInteger(seed.dailyReports.maxOf { it.reportId } + 1)

    override fun observeReports(): Flow<List<DailyReport>> =
        reports.map { list -> list.sortedWith(NEWEST_FIRST) }

    override fun observeReportsBy(userId: Int): Flow<List<DailyReport>> =
        reports.map { list -> list.filter { it.userId == userId }.sortedWith(NEWEST_FIRST) }

    override fun observeReport(reportId: Int): Flow<DailyReport?> =
        reports.map { list -> list.firstOrNull { it.reportId == reportId } }

    override suspend fun submitReport(draft: DailyReportDraft): DailyReport {
        val report = DailyReport(
            reportId = nextId.getAndIncrement(),
            projectId = draft.projectId,
            userId = draft.userId,
            date = LocalDate.now(clock),
            weather = draft.weather,
            workCompleted = draft.workCompleted,
            problemsEncountered = draft.problemsEncountered,
            status = ReportStatus.SUBMITTED,
            photoUris = draft.photoUris
        )
        reports.update { it + report }
        sync.recordChange(
            PendingChangeType.DAILY_REPORT,
            projects.findProject(draft.projectId)?.projectName.orEmpty()
        )
        notifier.notifyProjectManager(
            draft.projectId,
            NotificationType.REPORT,
            "${notifier.nameOf(draft.userId)} submitted a daily report for ${notifier.projectName(draft.projectId)}."
        )
        return report
    }

    override suspend fun approveReport(reportId: Int) {
        val report = reports.value.firstOrNull { it.reportId == reportId } ?: return
        if (report.status == ReportStatus.APPROVED) return
        reports.update { list ->
            list.map { if (it.reportId == reportId) it.copy(status = ReportStatus.APPROVED) else it }
        }
        notifier.notify(
            report.userId,
            NotificationType.REPORT,
            "${notifier.actorName()} approved your daily report for ${report.date.format(DAY)}."
        )
    }

    private companion object {
        val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
        val NEWEST_FIRST = compareByDescending<DailyReport> { it.date }
            .thenByDescending { it.reportId }
    }
}
