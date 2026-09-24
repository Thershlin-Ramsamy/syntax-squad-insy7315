package com.mcc.construction.ui.screens.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.format.formatHours
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val isLoading: Boolean = true,
    val openShift: Attendance? = null,
    /** Finished shifts, newest first. */
    val recentShifts: List<Attendance> = emptyList(),
    val hoursThisWeek: Double = 0.0,
    val shiftsThisWeek: Int = 0,
    val today: LocalDate = LocalDate.MIN,
    val isUpdating: Boolean = false,
    val message: UiMessage? = null
) {
    val isClockedIn: Boolean get() = openShift != null
}

/** Clock in, clock out, and a record of recent shifts. */
class AttendanceViewModel(
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val syncRepository: SyncRepository,
    private val clock: Clock
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val shifts = authRepository.session.flatMapLatest { session ->
        if (session == null) flowOf(null)
        else attendanceRepository.observeAttendance(session.user.userId)
    }

    val uiState: StateFlow<AttendanceUiState> = combine(shifts, transient) { all, extra ->
        val today = LocalDate.now(clock)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val closed = all.orEmpty().filter { !it.isOpen }
        val thisWeek = closed.filter { !it.date.isBefore(weekStart) }
        AttendanceUiState(
            isLoading = all == null,
            openShift = all.orEmpty().firstOrNull { it.isOpen },
            recentShifts = closed.take(RECENT_SHIFTS),
            hoursThisWeek = thisWeek.sumOf { it.totalHours ?: 0.0 },
            shiftsThisWeek = thisWeek.size,
            today = today,
            isUpdating = extra.isUpdating,
            message = extra.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AttendanceUiState())

    fun onClockInOut() {
        val userId = authRepository.session.value?.user?.userId ?: return
        if (transient.value.isUpdating) return
        transient.update { it.copy(isUpdating = true) }
        viewModelScope.launch {
            val offline = !syncRepository.status.value.isOnline
            val message = if (uiState.value.isClockedIn) {
                val shift = attendanceRepository.clockOut(userId)
                UiMessage(
                    if (offline) R.string.attendance_clocked_out_offline else R.string.attendance_clocked_out_message,
                    listOf(shift?.clockOut?.format(TIME).orEmpty(), formatHours(shift?.totalHours ?: 0.0))
                )
            } else {
                val shift = attendanceRepository.clockIn(userId)
                UiMessage(
                    if (offline) R.string.attendance_clocked_in_offline else R.string.attendance_clocked_in_message,
                    listOf(shift.clockIn.format(TIME))
                )
            }
            transient.update { it.copy(isUpdating = false, message = message) }
        }
    }

    fun onMessageShown() {
        transient.update { it.copy(message = null) }
    }

    private data class Transient(val isUpdating: Boolean = false, val message: UiMessage? = null)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val RECENT_SHIFTS = 7
        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                AttendanceViewModel(c.authRepository, c.attendanceRepository, c.syncRepository, c.clock)
            }
        }
    }
}
