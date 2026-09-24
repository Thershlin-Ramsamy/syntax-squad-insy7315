package com.mcc.construction.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.NotificationRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.UserSession
import com.mcc.construction.ui.common.appContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** What the navigation chrome needs, whichever screen is showing. */
data class AppUiState(
    val session: UserSession? = null,
    val syncStatus: SyncStatus = SyncStatus.Initial,
    val unreadNotifications: Int = 0
) {
    val role: UserRole? get() = session?.role
    val isSignedIn: Boolean get() = session != null
}

/**
 * State for the app shell: who is signed in (which picks the menu), the sync
 * status behind every indicator, and the unread count on the bell.
 */
class AppViewModel(
    private val authRepository: AuthRepository,
    syncRepository: SyncRepository,
    notificationRepository: NotificationRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val unreadCount = authRepository.session.flatMapLatest { session ->
        if (session == null) flowOf(0)
        else notificationRepository.observeUnreadCount(session.user.userId)
    }

    val uiState: StateFlow<AppUiState> = combine(
        authRepository.session,
        syncRepository.status,
        unreadCount
    ) { session, syncStatus, unread ->
        AppUiState(session = session, syncStatus = syncStatus, unreadNotifications = unread)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AppUiState(
            session = authRepository.session.value,
            syncStatus = syncRepository.status.value
        )
    )

    /**
     * The session right now, read straight from the repository. [uiState] can
     * trail a sign-in by a frame; navigation decisions use this instead.
     */
    fun hasActiveSession(): Boolean = authRepository.session.value != null

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AppViewModel(
                    container.authRepository,
                    container.syncRepository,
                    container.notificationRepository
                )
            }
        }
    }
}
