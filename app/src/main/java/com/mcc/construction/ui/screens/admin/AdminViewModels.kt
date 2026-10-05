package com.mcc.construction.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.*
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.launchSubmission
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.flow.*

private const val STOP_TIMEOUT_MS = 5_000L

data class AuditEntry(
    val log: AuditLog,
    val actorName: String
)

private fun List<AuditLog>.withNames(
    users: List<User>
): List<AuditEntry> {
    val names = users.associate { it.userId to it.fullName }
    return map {
        AuditEntry(it, names[it.userId].orEmpty())
    }
}

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    val activeByRole: Map<UserRole, Int> = emptyMap(),
    val suspended: List<User> = emptyList(),
    val recentActivity: List<AuditEntry> = emptyList(),
    val totalAuditEntries: Int = 0,
    val sync: SyncStatus? = null,
    val settings: SystemSettings = SystemSettings(),
    val now: LocalDateTime = LocalDateTime.MIN
) {
    val activeUsers: Int get() = activeByRole.values.sum()
}

class AdminDashboardViewModel(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    auditLogRepository: AuditLogRepository,
    syncRepository: SyncRepository,
    systemSettingsRepository: SystemSettingsRepository,
    private val clock: Clock
) : ViewModel() {

    private val people = combine(
        userRepository.observeUsers(),
        auditLogRepository.observeAuditLog()
    ) { users, audit ->
        users to audit
    }

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        authRepository.session,
        people,
        syncRepository.status,
        systemSettingsRepository.settings
    ) { session, (users, audit), sync, settings ->
        AdminDashboardUiState(
            isLoading = false,
            firstName = session?.user?.firstName.orEmpty(),
            activeByRole = UserRole.entries.associateWith { role ->
                users.count { it.role == role && it.isActive }
            },
            suspended = users.filter { !it.isActive },
            recentActivity = audit.take(5).withNames(users),
            totalAuditEntries = audit.size,
            sync = sync,
            settings = settings,
            now = LocalDateTime.now(clock)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        AdminDashboardUiState()
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                AdminDashboardViewModel(
                    c.authRepository,
                    c.userRepository,
                    c.auditLogRepository,
                    c.syncRepository,
                    c.systemSettingsRepository,
                    c.clock
                )
            }
        }
    }
}

enum class AccountFilter {
    ALL, ACTIVE, SUSPENDED
}

data class UserManagementUiState(
    val users: List<User>? = null,
    val currentUserId: Int? = null,
    val query: String = "",
    val roleFilter: UserRole? = null,
    val accountFilter: AccountFilter = AccountFilter.ALL,
    val selectedUserId: Int? = null,
    val confirmSuspendId: Int? = null,
    val isSaving: Boolean = false,
    val message: UiMessage? = null
) {
    val visible: List<User>
        get() {
            val q = query.trim()
            return users.orEmpty().filter { user ->
                (roleFilter == null || user.role == roleFilter) &&
                        (
                                when (accountFilter) {
                                    AccountFilter.ALL -> true
                                    AccountFilter.ACTIVE -> user.isActive
                                    AccountFilter.SUSPENDED -> !user.isActive
                                }
                                ) &&
                        (
                                q.isEmpty() ||
                                        user.fullName.contains(q, true) ||
                                        user.email.contains(q, true) ||
                                        user.employeeNumber.contains(q, true)
                                )
            }
        }

    fun countFor(role: UserRole?): Int =
        users.orEmpty().count { role == null || it.role == role }

    val selectedUser: User?
        get() = users?.firstOrNull { it.userId == selectedUserId }

    val paneUser: User?
        get() = selectedUser?.takeIf { selected ->
            visible.any { it.userId == selected.userId }
        } ?: visible.firstOrNull()

    fun isSelf(user: User): Boolean =
        user.userId == currentUserId
}

class UserManagementViewModel(
    authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val local = MutableStateFlow(UserManagementUiState())

    val uiState: StateFlow<UserManagementUiState> = combine(
        userRepository.observeUsers(),
        authRepository.session,
        local
    ) { users, session, current ->
        current.copy(
            users = users.sortedWith(
                compareBy({ it.role.ordinal }, { it.fullName })
            ),
            currentUserId = session?.user?.userId
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        UserManagementUiState()
    )

    fun onQueryChange(value: String) =
        local.update { it.copy(query = value) }

    fun onRoleFilterChange(role: UserRole?) =
        local.update { it.copy(roleFilter = role) }

    fun onAccountFilterChange(filter: AccountFilter) =
        local.update { it.copy(accountFilter = filter) }

    fun onSelectUser(userId: Int) {
        if (!local.value.isSaving) {
            local.update { it.copy(selectedUserId = userId) }
        }
    }

    fun onCloseUser() {
        if (!local.value.isSaving) {
            local.update { it.copy(selectedUserId = null) }
        }
    }

    fun onChangeRole(userId: Int, role: UserRole) {
        val user = uiState.value.users
            ?.firstOrNull { it.userId == userId } ?: return

        if (user.role == role || local.value.isSaving) return
        if (rejectSelf(user)) return

        save(
            UiMessage(
                R.string.users_role_changed,
                listOf(user.fullName)
            )
        ) {
            userRepository.assignRole(userId, role)
        }
    }

    fun onRequestSuspend(userId: Int) {
        if (local.value.isSaving) return

        val user = uiState.value.users
            ?.firstOrNull { it.userId == userId } ?: return

        if (rejectSelf(user)) return

        local.update { it.copy(confirmSuspendId = userId) }
    }

    fun onDismissSuspend() =
        local.update { it.copy(confirmSuspendId = null) }

    fun onConfirmSuspend() {
        val id = local.value.confirmSuspendId ?: return
        val user = uiState.value.users
            ?.firstOrNull { it.userId == id } ?: return

        if (rejectSelf(user)) return

        save(
            UiMessage(
                R.string.users_suspended_message,
                listOf(user.fullName)
            )
        ) {
            userRepository.setActive(id, false)
            local.update { it.copy(confirmSuspendId = null) }
        }
    }

    fun onRestore(userId: Int) {
        val user = uiState.value.users
            ?.firstOrNull { it.userId == userId } ?: return

        if (user.isActive) return

        save(
            UiMessage(
                R.string.users_restored_message,
                listOf(user.fullName)
            )
        ) {
            userRepository.setActive(userId, true)
        }
    }

    fun onMessageShown() =
        local.update { it.copy(message = null) }

    private fun rejectSelf(user: User): Boolean {
        if (!uiState.value.isSelf(user)) return false

        local.update {
            it.copy(
                message = UiMessage(
                    R.string.users_cannot_change_self,
                    isError = true
                )
            )
        }
        return true
    }

    private fun save(
        done: UiMessage,
        action: suspend () -> Unit
    ) {
        launchSubmission(
            isBusy = { local.value.isSaving },
            setBusy = { busy ->
                local.update { it.copy(isSaving = busy) }
            },
            failureMessage =
                "The account change could not be confirmed. Check your " +
                        "connection and the latest account details before retrying."
        ) {
            action()
            local.update { it.copy(message = done) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                UserManagementViewModel(
                    c.authRepository,
                    c.userRepository
                )
            }
        }
    }
}

data class AuditLogUiState(
    val entries: List<AuditEntry>? = null,
    val actors: List<User> = emptyList(),
    val actorFilter: Int? = null,
    val query: String = "",
    val now: LocalDateTime = LocalDateTime.MIN
) {
    val visible: List<AuditEntry>
        get() {
            val q = query.trim()
            return entries.orEmpty().filter {
                (actorFilter == null || it.log.userId == actorFilter) &&
                        (
                                q.isEmpty() ||
                                        it.log.action.contains(q, true) ||
                                        it.actorName.contains(q, true)
                                )
            }
        }
}

class AuditLogViewModel(
    auditLogRepository: AuditLogRepository,
    userRepository: UserRepository,
    private val clock: Clock
) : ViewModel() {

    private val filters = MutableStateFlow<Pair<Int?, String>>(null to "")

    val uiState: StateFlow<AuditLogUiState> = combine(
        auditLogRepository.observeAuditLog(),
        userRepository.observeUsers(),
        filters
    ) { log, users, (actor, query) ->
        val ids = log.map { it.userId }.toSet()

        AuditLogUiState(
            entries = log.withNames(users),
            actors = users.filter { it.userId in ids }
                .sortedBy { it.fullName },
            actorFilter = actor,
            query = query,
            now = LocalDateTime.now(clock)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        AuditLogUiState()
    )

    fun onActorFilterChange(userId: Int?) =
        filters.update { it.copy(first = userId) }

    fun onQueryChange(value: String) =
        filters.update { it.copy(second = value) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                AuditLogViewModel(
                    c.auditLogRepository,
                    c.userRepository,
                    c.clock
                )
            }
        }
    }
}

data class SystemSettingsUiState(
    val saved: SystemSettings = SystemSettings(),
    val draft: SystemSettings = SystemSettings(),
    val isSaving: Boolean = false,
    val message: UiMessage? = null
) {
    val hasChanges: Boolean get() = draft != saved
}

class SystemSettingsViewModel(
    private val repository: SystemSettingsRepository
) : ViewModel() {

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<SystemSettingsUiState> =
        combine(repository.settings, local) { saved, current ->
            SystemSettingsUiState(
                saved = saved,
                draft = current.draft ?: saved,
                isSaving = current.isSaving,
                message = current.message
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            SystemSettingsUiState()
        )

    fun onChange(change: (SystemSettings) -> SystemSettings) {
        local.update {
            if (it.isSaving) it
            else it.copy(
                draft = change(it.draft ?: repository.settings.value)
            )
        }
    }

    fun onDiscard() {
        if (local.value.isSaving) return
        local.update {
            it.copy(
                draft = null,
                message = UiMessage(R.string.system_discarded)
            )
        }
    }

    fun onSave() {
        val draft = local.value.draft ?: return

        launchSubmission(
            isBusy = { local.value.isSaving },
            setBusy = { busy ->
                local.update { it.copy(isSaving = busy) }
            },
            failureMessage =
                "System settings could not be confirmed. Your draft has " +
                        "been kept. Check your connection before trying again."
        ) {
            repository.update(draft)
            local.update {
                it.copy(
                    draft = null,
                    message = UiMessage(R.string.system_saved)
                )
            }
        }
    }

    fun onMessageShown() =
        local.update { it.copy(message = null) }

    private data class Local(
        val draft: SystemSettings? = null,
        val isSaving: Boolean = false,
        val message: UiMessage? = null
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SystemSettingsViewModel(
                    appContainer().systemSettingsRepository
                )
            }
        }
    }
}