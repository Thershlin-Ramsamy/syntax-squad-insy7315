package com.mcc.construction.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.Notification
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.NotificationRepository
import com.mcc.construction.ui.common.appContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NotificationFilter { ALL, UNREAD }

data class NotificationsUiState(
    /** Null until the first load finishes, so the screen can tell loading from empty. */
    val notifications: List<Notification>? = null,
    val filter: NotificationFilter = NotificationFilter.ALL
) {
    val unreadCount: Int get() = notifications?.count { !it.isRead } ?: 0

    val visible: List<Notification>
        get() = when (filter) {
            NotificationFilter.ALL -> notifications.orEmpty()
            NotificationFilter.UNREAD -> notifications.orEmpty().filter { !it.isRead }
        }
}

class NotificationsViewModel(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val filter = MutableStateFlow(NotificationFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val notifications = authRepository.session.flatMapLatest { session ->
        if (session == null) flowOf(emptyList())
        else notificationRepository.observeNotifications(session.user.userId)
    }

    val uiState: StateFlow<NotificationsUiState> =
        combine(notifications, filter) { list, selected ->
            NotificationsUiState(notifications = list, filter = selected)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = NotificationsUiState()
        )

    fun onFilterChange(selected: NotificationFilter) {
        filter.value = selected
    }

    /** Opening a notification counts as reading it. */
    fun onNotificationOpened(notification: Notification) {
        if (!notification.isRead) onMarkAsRead(notification.notificationId)
    }

    fun onMarkAsRead(notificationId: Int) {
        viewModelScope.launch { notificationRepository.markAsRead(notificationId) }
    }

    fun onMarkAllAsRead() {
        val userId = authRepository.session.value?.user?.userId ?: return
        viewModelScope.launch { notificationRepository.markAllAsRead(userId) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                NotificationsViewModel(container.authRepository, container.notificationRepository)
            }
        }
    }
}
