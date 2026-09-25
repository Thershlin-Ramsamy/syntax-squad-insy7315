package com.mcc.construction.ui.screens.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.screens.tasks.TaskListItem
import com.mcc.construction.ui.screens.tasks.TaskWorkOrder
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EmployeeProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val today: AttendanceToday = AttendanceToday.NotIn,
    /** Open tasks, most urgent first. */
    val currentTasks: List<TaskListItem> = emptyList(),
    val completedTasks: Int = 0,
    /** Shifts over the last fortnight, newest first. */
    val shifts: List<Attendance> = emptyList(),
    val hoursThisWeek: Double = 0.0,
    val hoursLastFortnight: Double = 0.0,
    val date: LocalDate = LocalDate.MIN
)

/** One employee: contact details, current work and attendance with hours. */
class EmployeeProfileViewModel(
    userId: Int,
    userRepository: UserRepository,
    taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
    attendanceRepository: AttendanceRepository,
    clock: Clock
) : ViewModel() {

    private val date = LocalDate.now(clock)

    val uiState: StateFlow<EmployeeProfileUiState> = combine(
        userRepository.observeUser(userId),
        taskRepository.observeTasksAssignedTo(userId),
        projectRepository.observeProjects(),
        attendanceRepository.observeAttendance(userId)
    ) { user, tasks, projects, attendance ->
        val names = projects.associate { it.projectId to it.projectName }
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val fortnight = attendance.filter { !it.date.isBefore(date.minusDays(HISTORY_DAYS)) }
        EmployeeProfileUiState(
            isLoading = false,
            user = user,
            today = AttendanceToday.from(attendance.filter { it.date == date }),
            currentTasks = tasks
                .filter { it.status != TaskStatus.COMPLETED }
                .sortedWith(TaskWorkOrder)
                .map { TaskListItem(it, names[it.projectId].orEmpty()) },
            completedTasks = tasks.count { it.status == TaskStatus.COMPLETED },
            shifts = fortnight.sortedByDescending { it.clockIn },
            hoursThisWeek = fortnight.filter { !it.date.isBefore(weekStart) }.sumOf { it.totalHours ?: 0.0 },
            hoursLastFortnight = fortnight.sumOf { it.totalHours ?: 0.0 },
            date = date
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EmployeeProfileUiState())

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val HISTORY_DAYS = 13L

        fun factory(userId: Int) = viewModelFactory {
            initializer {
                val c = appContainer()
                EmployeeProfileViewModel(
                    userId, c.userRepository, c.taskRepository, c.projectRepository, c.attendanceRepository, c.clock
                )
            }
        }
    }
}
