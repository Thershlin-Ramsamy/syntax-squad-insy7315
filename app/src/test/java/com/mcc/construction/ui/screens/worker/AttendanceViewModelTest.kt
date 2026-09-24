package com.mcc.construction.ui.screens.worker

import com.mcc.construction.R
import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttendanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(c: FakeAppContainer) =
        AttendanceViewModel(c.authRepository, c.attendanceRepository, c.syncRepository, c.clock)

    @Test
    fun startsClockedOutWithAWeekOfHistory() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)

        val state = vm.uiState.value
        assertFalse(state.isClockedIn)
        assertEquals(6, state.recentShifts.size)
        assertTrue(state.recentShifts.all { it.totalHours != null })
        assertEquals(state.recentShifts.sortedByDescending { it.clockIn }, state.recentShifts)
    }

    @Test
    fun clockInRecordsTheTimeAndClockOutWorksOutHours() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)

        vm.onClockInOut()
        assertNotNull(vm.uiState.value.openShift?.clockIn)
        assertEquals(R.string.attendance_clocked_in_message, vm.uiState.value.message?.textRes)

        vm.onClockInOut()
        val state = vm.uiState.value
        assertFalse(state.isClockedIn)
        assertNotNull(state.recentShifts.first().totalHours)
        assertEquals(R.string.attendance_clocked_out_message, state.message?.textRes)
    }

    @Test
    fun offlineClockInSaysItIsQueued() = runTest {
        val c = workerContainer()
        c.syncRepository.setOfflineMode(true)
        val vm = viewModel(c)
        keepCollecting(vm.uiState)

        vm.onClockInOut()

        assertEquals(R.string.attendance_clocked_in_offline, vm.uiState.value.message?.textRes)
    }
}
