package com.mcc.construction.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.User
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.NotificationRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.runBackendMutation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val unreadNotifications: Int = 0,
    val pendingChanges: Int = 0,
    val isSignOutDialogVisible: Boolean = false,
    val isSignedOut: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    notificationRepository: NotificationRepository,
    syncRepository: SyncRepository
) : ViewModel() {

    private val dialog = MutableStateFlow(DialogState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val unread = authRepository.session.flatMapLatest { session ->
        if (session == null) {
            flowOf(0)
        } else {
            notificationRepository.observeUnreadCount(
                session.user.userId
            )
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.session,
        unread,
        syncRepository.status,
        dialog
    ) { session, unreadCount, syncStatus, dialogState ->
        ProfileUiState(
            user = session?.user,
            unreadNotifications = unreadCount,
            pendingChanges = syncStatus.pendingCount,
            isSignOutDialogVisible = dialogState.isVisible,
            isSignedOut = dialogState.isSignedOut
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ProfileUiState(
            user = authRepository.session.value?.user
        )
    )

    fun onSignOutClick() {
        dialog.update { it.copy(isVisible = true) }
    }

    fun onSignOutDismiss() {
        dialog.update { it.copy(isVisible = false) }
    }

    fun onSignOutConfirm() {
        viewModelScope.launch {
            runBackendMutation {
                authRepository.signOut()

                dialog.update {
                    DialogState(
                        isVisible = false,
                        isSignedOut = true
                    )
                }
            }
        }
    }

    private data class DialogState(
        val isVisible: Boolean = false,
        val isSignedOut: Boolean = false
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ProfileViewModel(
                    container.authRepository,
                    container.notificationRepository,
                    container.syncRepository
                )
            }
        }
    }
}