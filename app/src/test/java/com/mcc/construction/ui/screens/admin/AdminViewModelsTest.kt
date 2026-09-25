package com.mcc.construction.ui.screens.admin

import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.fake.FakeSeedData
import com.mcc.construction.data.fake.testContainer
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.SignInResult
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.keepCollecting
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private suspend fun adminContainer(): FakeAppContainer = testContainer().also {
        it.authRepository.signIn(FakeSeedData.DEMO_ADMIN_EMAIL, "pw")
    }

    @Test
    fun dashboardCountsAccountsAndShowsRecentActivity() = runTest {
        val c = adminContainer()
        val vm = AdminDashboardViewModel(c.authRepository, c.userRepository, c.auditLogRepository, c.syncRepository, c.systemSettingsRepository, c.clock)
        keepCollecting(vm.uiState)
        val state = vm.uiState.value

        assertEquals("Riaan", state.firstName)
        assertEquals(6, state.activeByRole[UserRole.WORKER])
        assertEquals(listOf(9), state.suspended.map { it.userId })
        assertEquals(10, state.activeUsers)
        assertEquals("Riaan van Wyk", state.recentActivity.first().actorName)
    }

    @Test
    fun changingARoleIsSavedAndAudited() = runTest {
        val c = adminContainer()
        val vm = UserManagementViewModel(c.authRepository, c.userRepository)
        keepCollecting(vm.uiState)
        val before = c.auditLogRepository.observeAuditLog().first().size

        vm.onChangeRole(7, UserRole.MANAGER)

        assertEquals(UserRole.MANAGER, c.userRepository.observeUser(7).first()?.role)
        assertEquals(before + 1, c.auditLogRepository.observeAuditLog().first().size)
        assertFalse(vm.uiState.value.message!!.isError)
    }

    @Test
    fun suspendingAsksFirstThenBlocksSignIn() = runTest {
        val c = adminContainer()
        val vm = UserManagementViewModel(c.authRepository, c.userRepository)
        keepCollecting(vm.uiState)

        vm.onRequestSuspend(5)
        assertEquals(5, vm.uiState.value.confirmSuspendId)
        assertTrue(c.userRepository.observeUser(5).first()!!.isActive)

        vm.onConfirmSuspend()
        assertNull(vm.uiState.value.confirmSuspendId)
        assertFalse(c.userRepository.observeUser(5).first()!!.isActive)
        c.authRepository.signOut()
        assertEquals(SignInResult.AccountSuspended, c.authRepository.signIn(FakeSeedData.DEMO_WORKER_EMAIL, "pw"))
    }

    @Test
    fun restoringLetsTheUserBackIn() = runTest {
        val c = adminContainer()
        val vm = UserManagementViewModel(c.authRepository, c.userRepository)
        keepCollecting(vm.uiState)

        vm.onAccountFilterChange(AccountFilter.SUSPENDED)
        assertEquals(listOf(9), vm.uiState.value.visible.map { it.userId })
        vm.onRestore(9)

        assertTrue(c.userRepository.observeUser(9).first()!!.isActive)
        assertTrue(vm.uiState.value.visible.isEmpty())
    }

    @Test
    fun administratorsCannotChangeTheirOwnAccount() = runTest {
        val c = adminContainer()
        val vm = UserManagementViewModel(c.authRepository, c.userRepository)
        keepCollecting(vm.uiState)

        vm.onChangeRole(1, UserRole.WORKER)
        vm.onRequestSuspend(1)

        assertEquals(UserRole.ADMINISTRATOR, c.userRepository.observeUser(1).first()?.role)
        assertNull(vm.uiState.value.confirmSuspendId)
        assertTrue(vm.uiState.value.message!!.isError)
    }

    @Test
    fun userSearchMatchesNameEmailAndEmployeeNumber() = runTest {
        val c = adminContainer()
        val vm = UserManagementViewModel(c.authRepository, c.userRepository)
        keepCollecting(vm.uiState)

        vm.onQueryChange("mcc-2041")
        assertEquals(listOf(5), vm.uiState.value.visible.map { it.userId })
        vm.onQueryChange("naledi")
        assertEquals(listOf(2), vm.uiState.value.visible.map { it.userId })
    }

    @Test
    fun auditLogFiltersByPersonAndText() = runTest {
        val c = adminContainer()
        val vm = AuditLogViewModel(c.auditLogRepository, c.userRepository, c.clock)
        keepCollecting(vm.uiState)

        vm.onActorFilterChange(1)
        assertTrue(vm.uiState.value.visible.all { it.log.userId == 1 })
        vm.onQueryChange("suspended")
        assertEquals(1, vm.uiState.value.visible.size)
    }

    @Test
    fun systemSettingsSaveOnlyWhenAskedAndCanBeDiscarded() = runTest {
        val c = adminContainer()
        val vm = SystemSettingsViewModel(c.systemSettingsRepository)
        keepCollecting(vm.uiState)
        assertFalse(vm.uiState.value.hasChanges)

        vm.onChange { it.copy(syncIntervalMinutes = 5) }
        assertTrue(vm.uiState.value.hasChanges)
        assertEquals(15, c.systemSettingsRepository.settings.value.syncIntervalMinutes)
        vm.onDiscard()
        assertFalse(vm.uiState.value.hasChanges)

        vm.onChange { it.copy(requireDailyChecklist = false) }
        vm.onSave()
        assertFalse(c.systemSettingsRepository.settings.value.requireDailyChecklist)
        assertFalse(vm.uiState.value.hasChanges)
        assertTrue(c.auditLogRepository.observeAuditLog().first().first().action.contains("daily safety checklist"))
    }
}
