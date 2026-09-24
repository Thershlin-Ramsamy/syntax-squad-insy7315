package com.mcc.construction.ui.screens.dashboard

import com.mcc.construction.R
import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.WORKER_ID
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkerDashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(c: FakeAppContainer) = WorkerDashboardViewModel(
        c.authRepository, c.taskRepository, c.projectRepository, c.attendanceRepository,
        c.notificationRepository, c.safetyRepository, c.clock
    )

    @Test
    fun showsTheWorkersDay() = runTest {
        val c = workerContainer()
        val vm = viewModel(c)
        keepCollecting(vm.uiState)

        val state = vm.uiState.value
        assertEquals("Sipho", state.firstName)
        assertFalse(state.isClockedIn)
        assertEquals(1, state.inProgressCount)
        assertTrue(state.nextTasks.isNotEmpty())
        assertEquals(TaskStatus.IN_PROGRESS, state.nextTasks.first().task.status)
        assertEquals(2, state.unreadNotifications)
    }

    @Test
    fun clockInAndOutFromTheDashboard() = runTest {
        val c = workerContainer()
        val vm = viewModel(c)
        keepCollecting(vm.uiState)

        vm.onClockInOut()
        assertTrue(vm.uiState.value.isClockedIn)
        assertEquals(R.string.attendance_clocked_in_message, vm.uiState.value.message?.textRes)

        vm.onMessageShown()
        vm.onClockInOut()
        assertFalse(vm.uiState.value.isClockedIn)
        assertEquals(R.string.attendance_clocked_out_message, vm.uiState.value.message?.textRes)
        assertNull(c.attendanceRepository.clockOut(WORKER_ID))
    }

    @Test
    fun completingATaskElsewhereUpdatesTheCounts() = runTest {
        val c = workerContainer()
        val vm = viewModel(c)
        keepCollecting(vm.uiState)
        val before = vm.uiState.value.completedCount

        c.taskRepository.updateStatus(101, TaskStatus.COMPLETED)

        assertEquals(before + 1, vm.uiState.value.completedCount)
        assertEquals(0, vm.uiState.value.inProgressCount)
    }

    @Test
    fun greetingFollowsTheTimeOfDay() {
        assertEquals(DayPart.MORNING, WorkerDashboardViewModel.dayPartOf(LocalTime.of(7, 0)))
        assertEquals(DayPart.AFTERNOON, WorkerDashboardViewModel.dayPartOf(LocalTime.of(13, 0)))
        assertEquals(DayPart.EVENING, WorkerDashboardViewModel.dayPartOf(LocalTime.of(18, 30)))
    }
}
