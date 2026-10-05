package com.mcc.construction.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.runBackendMutation
import com.mcc.construction.ui.format.formatHours
import com.mcc.construction.ui.screens.tasks.TaskListItem
import com.mcc.construction.ui.screens.tasks.TaskWorkOrder
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class DayPart {
    MORNING, AFTERNOON, EVENING
}

data class WorkerDashboardUiState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    val dayPart: DayPart = DayPart.MORNING,
    val today: LocalDate = LocalDate.MIN,
    val siteName: String? = null,
    val pendingCount: Int = 0,
    val inProgressCount: Int = 0,
    val completedCount: Int = 0,
    val nextTasks: List<TaskListItem> = emptyList(),
    val openShift: Attendance? = null,
    val isClockUpdating: Boolean = false,
    val unreadNotifications: Int = 0,
    val checklistDoneToday: Boolean = false,
    val message: UiMessage? = null
) {
    val isClockedIn: Boolean
        get() = openShift != null

    val openCount: Int
        get() = pendingCount + inProgressCount
}

class WorkerDashboardViewModel(
    private val authRepository: AuthRepository,
    taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
    private val attendanceRepository: AttendanceRepository,
    notificationRepository: NotificationRepository,
    safetyRepository: SafetyRepository,
    private val clock: Clock
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workerData = authRepository.session.flatMapLatest { session ->
        if (session == null) return@flatMapLatest flowOf(null)

        val userId = session.user.userId

        combine(
            taskRepository.observeTasksAssignedTo(userId),
            projectRepository.observeProjects(),
            attendanceRepository.observeOpenShift(userId),
            notificationRepository.observeUnreadCount(userId),
            safetyRepository.observeInspectionsBy(userId)
        ) { tasks, projects, openShift, unread, inspections ->
            val today = LocalDate.now(clock)
            val projectNames = projects.associate {
                it.projectId to it.projectName
            }
            val todays = tasks.filter {
                !it.deadline.isAfter(today) ||
                        it.status == TaskStatus.IN_PROGRESS
            }
            val next = todays.filter {
                it.status != TaskStatus.COMPLETED
            }.sortedWith(TaskWorkOrder)

            WorkerDashboardUiState(
                isLoading = false,
                firstName = session.user.firstName,
                dayPart = dayPartOf(LocalTime.now(clock)),
                today = today,
                siteName = next.firstOrNull()?.let {
                    projectNames[it.projectId]
                } ?: tasks.firstOrNull()?.let {
                    projectNames[it.projectId]
                },
                pendingCount = todays.count {
                    it.status == TaskStatus.PENDING
                },
                inProgressCount = todays.count {
                    it.status == TaskStatus.IN_PROGRESS
                },
                completedCount = todays.count {
                    it.status == TaskStatus.COMPLETED
                },
                nextTasks = next.take(NEXT_TASKS).map {
                    TaskListItem(it, projectNames[it.projectId].orEmpty())
                },
                openShift = openShift,
                unreadNotifications = unread,
                checklistDoneToday = inspections.any {
                    it.date == today
                }
            )
        }
    }

    val uiState: StateFlow<WorkerDashboardUiState> =
        combine(workerData, transient) { data, extra ->
            (data ?: WorkerDashboardUiState()).copy(
                isClockUpdating = extra.isClockUpdating,
                message = extra.message
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            WorkerDashboardUiState()
        )

    fun onClockInOut() {
        val userId = authRepository.session.value?.user?.userId ?: return
        if (transient.value.isClockUpdating) return

        transient.update { it.copy(isClockUpdating = true) }

        viewModelScope.launch {
            runBackendMutation(
                onFinally = {
                    transient.update {
                        it.copy(isClockUpdating = false)
                    }
                }
            ) {
                val message = if (uiState.value.isClockedIn) {
                    val shift = attendanceRepository.clockOut(userId)

                    UiMessage(
                        R.string.attendance_clocked_out_message,
                        listOf(
                            shift?.clockOut?.format(TIME).orEmpty(),
                            formatHours(shift?.totalHours ?: 0.0)
                        )
                    )
                } else {
                    val shift = attendanceRepository.clockIn(userId)

                    UiMessage(
                        R.string.attendance_clocked_in_message,
                        listOf(shift.clockIn.format(TIME))
                    )
                }

                transient.update {
                    it.copy(
                        isClockUpdating = false,
                        message = message
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        transient.update { it.copy(message = null) }
    }

    private data class Transient(
        val isClockUpdating: Boolean = false,
        val message: UiMessage? = null
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val NEXT_TASKS = 3

        private val TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm")

        fun dayPartOf(time: LocalTime): DayPart = when (time.hour) {
            in 0..11 -> DayPart.MORNING
            in 12..16 -> DayPart.AFTERNOON
            else -> DayPart.EVENING
        }

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                WorkerDashboardViewModel(
                    c.authRepository,
                    c.taskRepository,
                    c.projectRepository,
                    c.attendanceRepository,
                    c.notificationRepository,
                    c.safetyRepository,
                    c.clock
                )
            }
        }
    }
}