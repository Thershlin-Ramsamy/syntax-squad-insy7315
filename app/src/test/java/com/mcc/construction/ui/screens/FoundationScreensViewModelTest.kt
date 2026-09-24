package com.mcc.construction.ui.screens

import com.mcc.construction.data.fake.testContainer
import com.mcc.construction.data.model.SyncState
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.notifications.NotificationFilter
import com.mcc.construction.ui.screens.notifications.NotificationsViewModel
import com.mcc.construction.ui.screens.profile.ProfileViewModel
import com.mcc.construction.ui.screens.settings.SettingsViewModel
import com.mcc.construction.ui.screens.sync.SyncStatusViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The Profile, Settings, Offline Sync and Notifications ViewModels, run against
 * the fake repositories with the demo worker signed in.
 */
class FoundationScreensViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val container = testContainer()

    /** `WhileSubscribed` state only updates while something collects it. */
    private fun TestScope.keepCollecting(state: StateFlow<*>) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { state.collect {} }
    }

    private suspend fun signInWorker() {
        container.authRepository.signIn("worker@mcc.co.za", "pw")
    }

    // --- Notifications -----------------------------------------------------

    @Test
    fun notificationsShowOnlyTheSignedInUsersNewestFirst() = runTest {
        signInWorker()
        val viewModel = NotificationsViewModel(container.authRepository, container.notificationRepository)
        keepCollecting(viewModel.uiState)

        val list = viewModel.uiState.value.notifications.orEmpty()
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.userId == 5 })
        assertEquals(list.sortedByDescending { it.sentDate }, list)
    }

    @Test
    fun openingANotificationMarksItRead() = runTest {
        signInWorker()
        val viewModel = NotificationsViewModel(container.authRepository, container.notificationRepository)
        keepCollecting(viewModel.uiState)
        val before = viewModel.uiState.value.unreadCount
        val unread = viewModel.uiState.value.visible.first { !it.isRead }

        viewModel.onNotificationOpened(unread)

        assertEquals(before - 1, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun unreadFilterAndMarkAllRead() = runTest {
        signInWorker()
        val viewModel = NotificationsViewModel(container.authRepository, container.notificationRepository)
        keepCollecting(viewModel.uiState)

        viewModel.onFilterChange(NotificationFilter.UNREAD)
        assertTrue(viewModel.uiState.value.visible.all { !it.isRead })

        viewModel.onMarkAllAsRead()
        assertEquals(0, viewModel.uiState.value.unreadCount)
        assertTrue(viewModel.uiState.value.visible.isEmpty())
    }

    // --- Offline sync ------------------------------------------------------

    @Test
    fun syncNowClearsTheQueueAndReportsTheCount() = runTest {
        signInWorker()
        val viewModel = SyncStatusViewModel(container.syncRepository)
        keepCollecting(viewModel.uiState)
        assertTrue(viewModel.uiState.value.canSyncNow)

        viewModel.onSyncNow()

        val state = viewModel.uiState.value
        assertEquals(SyncState.SYNCED, state.status.state)
        assertEquals(2, state.lastUploadCount)
        assertFalse(state.canSyncNow)
    }

    @Test
    fun offlineModeDisablesSyncNow() = runTest {
        val viewModel = SyncStatusViewModel(container.syncRepository)
        keepCollecting(viewModel.uiState)

        viewModel.onOfflineModeChange(true)

        assertEquals(SyncState.OFFLINE, viewModel.uiState.value.status.state)
        assertFalse(viewModel.uiState.value.canSyncNow)
    }

    // --- Settings ----------------------------------------------------------

    @Test
    fun settingsTogglesReachTheRepositories() = runTest {
        signInWorker()
        val viewModel = SettingsViewModel(
            container.settingsRepository,
            container.syncRepository,
            container.authRepository,
            appVersion = "1.0 (1)",
            isDemoData = true
        )
        keepCollecting(viewModel.uiState)

        viewModel.onNotificationsChange(false)
        viewModel.onOfflineModeChange(true)

        val state = viewModel.uiState.value
        assertFalse(state.notificationsEnabled)
        assertTrue(state.isOfflineMode)
        assertTrue(container.syncRepository.status.value.isOfflineMode)
        assertEquals("1.0 (1)", state.appVersion)
    }

    // --- Profile -----------------------------------------------------------

    @Test
    fun profileShowsTheSignedInUserAndSignsOutAfterConfirming() = runTest {
        signInWorker()
        val viewModel = ProfileViewModel(
            container.authRepository,
            container.notificationRepository,
            container.syncRepository
        )
        keepCollecting(viewModel.uiState)
        assertEquals("Sipho Dlamini", viewModel.uiState.value.user?.fullName)

        viewModel.onSignOutClick()
        assertTrue(viewModel.uiState.value.isSignOutDialogVisible)

        viewModel.onSignOutConfirm()
        assertTrue(viewModel.uiState.value.isSignedOut)
        assertNull(container.authRepository.session.value)
    }

    @Test
    fun cancellingSignOutKeepsTheSession() = runTest {
        signInWorker()
        val viewModel = ProfileViewModel(
            container.authRepository,
            container.notificationRepository,
            container.syncRepository
        )
        keepCollecting(viewModel.uiState)

        viewModel.onSignOutClick()
        viewModel.onSignOutDismiss()

        assertFalse(viewModel.uiState.value.isSignOutDialogVisible)
        assertEquals(5, container.authRepository.session.value?.user?.userId)
    }
}
