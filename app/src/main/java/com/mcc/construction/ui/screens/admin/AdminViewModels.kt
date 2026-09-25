package com.mcc.construction.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.AuditLog
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.model.SystemSettings
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuditLogRepository
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.SystemSettingsRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

/** An audit entry with the name of whoever did it. */
data class AuditEntry(val log: AuditLog, val actorName: String)

private fun List<AuditLog>.withNames(users: List<User>): List<AuditEntry> {
    val names = users.associate { it.userId to it.fullName }
    return map { AuditEntry(it, names[it.userId].orEmpty()) }
}

// ---------------------------------------------------------------------------
// Administration dashboard
// ---------------------------------------------------------------------------

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    /** Active accounts per role. */
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

/** The administrator's overview: accounts, recent changes, sync health and key settings. */
class AdminDashboardViewModel(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    auditLogRepository: AuditLogRepository,
    syncRepository: SyncRepository,
    systemSettingsRepository: SystemSettingsRepository,
    private val clock: Clock
) : ViewModel() {

    private val people = combine(userRepository.observeUsers(), auditLogRepository.observeAuditLog()) { u, a -> u to a }

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        authRepository.session,
        people,
        syncRepository.status,
        systemSettingsRepository.settings
    ) { session, (users, audit), sync, settings ->
        AdminDashboardUiState(
            isLoading = false,
            firstName = session?.user?.firstName.orEmpty(),
            activeByRole = UserRole.entries.associateWith { role -> users.count { it.role == role && it.isActive } },
            suspended = users.filter { !it.isActive },
            recentActivity = audit.take(RECENT_ACTIVITY).withNames(users),
            totalAuditEntries = audit.size,
            sync = sync,
            settings = settings,
            now = LocalDateTime.now(clock)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AdminDashboardUiState())

    companion object {
        private const val RECENT_ACTIVITY = 5

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                AdminDashboardViewModel(
                    c.authRepository, c.userRepository, c.auditLogRepository, c.syncRepository,
                    c.systemSettingsRepository, c.clock
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// User management
// ---------------------------------------------------------------------------

/** Which accounts the user list shows. */
enum class AccountFilter { ALL, ACTIVE, SUSPENDED }

data class UserManagementUiState(
    /** Null until the first load. */
    val users: List<User>? = null,
    val currentUserId: Int? = null,
    val query: String = "",
    val roleFilter: UserRole? = null,
    val accountFilter: AccountFilter = AccountFilter.ALL,
    /** The account open for changes: in the side pane on wide windows, a dialog on phones. */
    val selectedUserId: Int? = null,
    /** Set while asking to confirm a suspension. */
    val confirmSuspendId: Int? = null,
    val isSaving: Boolean = false,
    val message: UiMessage? = null
) {
    val visible: List<User>
        get() {
            val q = query.trim()
            return users.orEmpty().filter { user ->
                (roleFilter == null || user.role == roleFilter) &&
                    when (accountFilter) {
                        AccountFilter.ALL -> true
                        AccountFilter.ACTIVE -> user.isActive
                        AccountFilter.SUSPENDED -> !user.isActive
                    } &&
                    (q.isEmpty() || user.fullName.contains(q, true) || user.email.contains(q, true) ||
                        user.employeeNumber.contains(q, true))
            }
        }

    fun countFor(role: UserRole?): Int = users.orEmpty().count { role == null || it.role == role }

    val selectedUser: User? get() = users?.firstOrNull { it.userId == selectedUserId }

    /** The account the side pane shows: the chosen one, or the first in the list. */
    val paneUser: User?
        get() = selectedUser?.takeIf { u -> visible.any { it.userId == u.userId } } ?: visible.firstOrNull()

    /** Administrators cannot lock themselves out or remove their own rights. */
    fun isSelf(user: User): Boolean = user.userId == currentUserId
}

/** Accounts: search, change a role, suspend or restore. Every change goes into the audit log. */
class UserManagementViewModel(
    authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val local = MutableStateFlow(UserManagementUiState())

    val uiState: StateFlow<UserManagementUiState> = combine(
        userRepository.observeUsers(),
        authRepository.session,
        local
    ) { users, session, l ->
        l.copy(
            users = users.sortedWith(compareBy({ it.role.ordinal }, { it.fullName })),
            currentUserId = session?.user?.userId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserManagementUiState())

    fun onQueryChange(value: String) = local.update { it.copy(query = value) }

    fun onRoleFilterChange(role: UserRole?) = local.update { it.copy(roleFilter = role) }

    fun onAccountFilterChange(filter: AccountFilter) = local.update { it.copy(accountFilter = filter) }

    fun onSelectUser(userId: Int) = local.update { it.copy(selectedUserId = userId) }

    fun onCloseUser() = local.update { it.copy(selectedUserId = null) }

    fun onChangeRole(userId: Int, role: UserRole) {
        val user = uiState.value.users?.firstOrNull { it.userId == userId } ?: return
        if (user.role == role || local.value.isSaving) return
        if (uiState.value.isSelf(user)) {
            local.update { it.copy(message = UiMessage(R.string.users_cannot_change_self, isError = true)) }
            return
        }
        save(UiMessage(R.string.users_role_changed, listOf(user.fullName))) { userRepository.assignRole(userId, role) }
    }

    fun onRequestSuspend(userId: Int) {
        val user = uiState.value.users?.firstOrNull { it.userId == userId } ?: return
        if (uiState.value.isSelf(user)) {
            local.update { it.copy(message = UiMessage(R.string.users_cannot_change_self, isError = true)) }
            return
        }
        local.update { it.copy(confirmSuspendId = userId) }
    }

    fun onDismissSuspend() = local.update { it.copy(confirmSuspendId = null) }

    fun onConfirmSuspend() {
        val id = local.value.confirmSuspendId ?: return
        val user = uiState.value.users?.firstOrNull { it.userId == id } ?: return
        local.update { it.copy(confirmSuspendId = null) }
        save(UiMessage(R.string.users_suspended_message, listOf(user.fullName))) { userRepository.setActive(id, false) }
    }

    fun onRestore(userId: Int) {
        val user = uiState.value.users?.firstOrNull { it.userId == userId } ?: return
        save(UiMessage(R.string.users_restored_message, listOf(user.fullName))) { userRepository.setActive(userId, true) }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    private fun save(done: UiMessage, action: suspend () -> Unit) {
        local.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            action()
            local.update { it.copy(isSaving = false, message = done) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                UserManagementViewModel(c.authRepository, c.userRepository)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Audit log
// ---------------------------------------------------------------------------

data class AuditLogUiState(
    /** Null until the first load. Newest first. */
    val entries: List<AuditEntry>? = null,
    /** People who appear in the log, for the filter. */
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
                    (q.isEmpty() || it.log.action.contains(q, true) || it.actorName.contains(q, true))
            }
        }
}

/** Every recorded administrative action, searchable and filterable by person. */
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
            actors = users.filter { it.userId in ids }.sortedBy { it.fullName },
            actorFilter = actor,
            query = query,
            now = LocalDateTime.now(clock)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AuditLogUiState())

    fun onActorFilterChange(userId: Int?) = filters.update { it.copy(first = userId) }

    fun onQueryChange(value: String) = filters.update { it.copy(second = value) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                AuditLogViewModel(c.auditLogRepository, c.userRepository, c.clock)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// System settings
// ---------------------------------------------------------------------------

data class SystemSettingsUiState(
    val saved: SystemSettings = SystemSettings(),
    /** What is on screen; differs from [saved] until the change is saved. */
    val draft: SystemSettings = SystemSettings(),
    val isSaving: Boolean = false,
    val message: UiMessage? = null
) {
    val hasChanges: Boolean get() = draft != saved
}

/** Organisation-wide settings, edited as a draft and saved together. */
class SystemSettingsViewModel(
    private val repository: SystemSettingsRepository
) : ViewModel() {

    /** Null draft means "same as saved". */
    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<SystemSettingsUiState> = combine(repository.settings, local) { saved, l ->
        SystemSettingsUiState(saved = saved, draft = l.draft ?: saved, isSaving = l.isSaving, message = l.message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SystemSettingsUiState())

    fun onChange(change: (SystemSettings) -> SystemSettings) {
        local.update { it.copy(draft = change(it.draft ?: repository.settings.value)) }
    }

    fun onDiscard() = local.update { it.copy(draft = null, message = UiMessage(R.string.system_discarded)) }

    fun onSave() {
        val draft = local.value.draft ?: return
        if (local.value.isSaving) return
        local.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.update(draft)
            local.update { Local(message = UiMessage(R.string.system_saved)) }
        }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    private data class Local(
        val draft: SystemSettings? = null,
        val isSaving: Boolean = false,
        val message: UiMessage? = null
    )

    companion object {
        val Factory = viewModelFactory {
            initializer { SystemSettingsViewModel(appContainer().systemSettingsRepository) }
        }
    }
}
