package com.mcc.construction.ui.screens.worker

import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.Weather
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.DailyReportDraft
import com.mcc.construction.data.repository.DailyReportRepository
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BackendFailureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun attendanceFailureReenablesTheButtonAndAllowsRetry() = runTest {
        val c = workerContainer()
        var attempts = 0

        val repository =
            object : AttendanceRepository by c.attendanceRepository {
                override suspend fun clockIn(userId: Int): Attendance {
                    attempts++

                    if (attempts == 1) {
                        throw IOException("Lost connection")
                    }

                    return c.attendanceRepository.clockIn(userId)
                }
            }

        val vm = AttendanceViewModel(
            c.authRepository,
            repository,
            c.syncRepository,
            c.clock
        )

        keepCollecting(vm.uiState)

        vm.onClockInOut()

        assertFalse(vm.uiState.value.isUpdating)
        assertNull(vm.uiState.value.openShift)

        vm.onClockInOut()

        assertEquals(2, attempts)
        assertNotNull(vm.uiState.value.openShift)
    }

    @Test
    fun reportFailureRetainsDraftAndDoesNotShowSubmitted() = runTest {
        val c = workerContainer()

        val repository =
            object : DailyReportRepository by c.dailyReportRepository {
                override suspend fun submitReport(
                    draft: DailyReportDraft
                ): DailyReport {
                    throw IOException("Server unavailable")
                }
            }

        val vm = DailyReportViewModel(
            c.authRepository,
            c.projectRepository,
            c.taskRepository,
            repository,
            c.syncRepository,
            c.clock
        )

        keepCollecting(vm.uiState)

        vm.onWeatherSelected(Weather.entries.first())
        vm.onWorkCompletedChange("Installed the roof supports")
        vm.onProblemsChange("Waiting for materials")
        vm.onConfirmSubmit()

        assertFalse(vm.uiState.value.form.isSubmitting)
        assertNull(vm.uiState.value.form.submitted)

        assertEquals(
            "Installed the roof supports",
            vm.uiState.value.form.workCompleted
        )

        assertEquals(
            "Waiting for materials",
            vm.uiState.value.form.problems
        )
    }
}