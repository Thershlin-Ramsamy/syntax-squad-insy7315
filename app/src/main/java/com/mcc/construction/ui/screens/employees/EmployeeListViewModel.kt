package com.mcc.construction.ui.screens.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.appContainer
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** An employee with today's attendance and how much open work they have. */
data class EmployeeItem(val user: User, val today: AttendanceToday, val openTasks: Int)

data class EmployeeListUiState(
    /** Null until the first load. */
    val employees: List<EmployeeItem>? = null,
    val query: String = "",
    /** Null shows every role. */
    val roleFilter: UserRole? = null,
    val selectedEmployeeId: Int? = null
) {
    val visible: List<EmployeeItem>
        get() {
            val q = query.trim()
            return employees.orEmpty().filter { item ->
                (roleFilter == null || item.user.role == roleFilter) &&
                    (q.isEmpty() || item.user.fullName.contains(q, ignoreCase = true) ||
                        item.user.jobTitle.contains(q, ignoreCase = true) ||
                        item.user.employeeNumber.contains(q, ignoreCase = true))
            }
        }

    fun countFor(role: UserRole?): Int = employees.orEmpty().count { role == null || it.user.role == role }

    /** Today's roll call for the people currently listed. */
    fun countWith(status: PresenceStatus): Int = visible.count { it.today.status == status }

    val paneEmployeeId: Int?
        get() = selectedEmployeeId?.takeIf { id -> visible.any { it.user.userId == id } }
            ?: visible.firstOrNull()?.user?.userId
}

/** The workforce with today's attendance, searchable by name. */
class EmployeeListViewModel(
    userRepository: UserRepository,
    attendanceRepository: AttendanceRepository,
    taskRepository: TaskRepository,
    clock: Clock
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val roleFilter = MutableStateFlow<UserRole?>(UserRole.WORKER)
    private val selected = MutableStateFlow<Int?>(null)

    private val employees = combine(
        userRepository.observeUsers(),
        attendanceRepository.observeAttendanceOn(LocalDate.now(clock)),
        taskRepository.observeTasks(),
        taskRepository.observeAllAssignments()
    ) { users, attendance, tasks, assignments ->
        val open = tasks.filter { it.status != TaskStatus.COMPLETED }.map { it.taskId }.toSet()
        val shifts = attendance.groupBy { it.userId }
        users
            .filter { it.role != UserRole.ADMINISTRATOR }
            .sortedWith(compareBy({ !it.isActive }, { it.fullName }))
            .map { user ->
                EmployeeItem(
                    user = user,
                    today = AttendanceToday.from(shifts[user.userId].orEmpty()),
                    openTasks = assignments.count { it.userId == user.userId && it.taskId in open }
                )
            }
    }

    val uiState: StateFlow<EmployeeListUiState> = combine(employees, query, roleFilter, selected) { list, q, role, id ->
        EmployeeListUiState(employees = list, query = q, roleFilter = role, selectedEmployeeId = id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EmployeeListUiState())

    fun onQueryChange(value: String) = query.update { value }

    fun onRoleFilterChange(role: UserRole?) = roleFilter.update { role }

    fun onSelectEmployee(userId: Int) = selected.update { userId }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                EmployeeListViewModel(c.userRepository, c.attendanceRepository, c.taskRepository, c.clock)
            }
        }
    }
}
