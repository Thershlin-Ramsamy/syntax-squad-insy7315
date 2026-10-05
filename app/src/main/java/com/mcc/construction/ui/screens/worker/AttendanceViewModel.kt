package com.mcc.construction.ui.screens.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.mcc.construction.R
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.launchSubmission
import com.mcc.construction.ui.format.formatHours
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class AttendanceUiState(
    val isLoading: Boolean = true,
    val openShift: Attendance? = null,
    val recentShifts: List<Attendance> = emptyList(),
    val hoursThisWeek: Double = 0.0,
    val shiftsThisWeek: Int = 0,
    val today: LocalDate = LocalDate.MIN,
    val isUpdating: Boolean = false,
    val message: UiMessage? = null
) {
    val isClockedIn: Boolean get() = openShift != null
}

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

    val uiState: StateFlow<AttendanceUiState> =
        combine(shifts, transient) { all, extra ->
            val today = LocalDate.now(clock)
            val weekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            )
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
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            AttendanceUiState()
        )

    fun onClockInOut() {
        val userId = authRepository.session.value?.user?.userId ?: return
        val state = uiState.value
        if (state.isLoading) return

        val wasClockedIn = state.isClockedIn

        launchSubmission(
            isBusy = { transient.value.isUpdating },
            setBusy = { busy ->
                transient.update { it.copy(isUpdating = busy) }
            },
            failureMessage =
                "Attendance could not be confirmed. Check your connection " +
                        "and shift history before trying again."
        ) {
            val feedback = if (wasClockedIn) {
                val shift = attendanceRepository.clockOut(userId)
                    ?: error("No shift was returned.")

                UiMessage(
                    R.string.attendance_clocked_out_message,
                    listOf(
                        shift.clockOut?.format(TIME).orEmpty(),
                        formatHours(shift.totalHours ?: 0.0)
                    )
                )
            } else {
                val shift = attendanceRepository.clockIn(userId)

                UiMessage(
                    R.string.attendance_clocked_in_message,
                    listOf(shift.clockIn.format(TIME))
                )
            }

            if (authRepository.session.value?.user?.userId == userId) {
                transient.update { it.copy(message = feedback) }
            }
        }
    }

    fun onMessageShown() {
        transient.update { it.copy(message = null) }
    }

    private data class Transient(
        val isUpdating: Boolean = false,
        val message: UiMessage? = null
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val RECENT_SHIFTS = 7
        private val TIME = DateTimeFormatter.ofPattern("HH:mm")

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                AttendanceViewModel(
                    c.authRepository,
                    c.attendanceRepository,
                    c.syncRepository,
                    c.clock
                )
            }
        }
    }
}