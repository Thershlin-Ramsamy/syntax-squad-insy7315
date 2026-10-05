package com.mcc.construction.data.remote

import com.mcc.construction.data.model.OfflineSync
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.model.SystemSettings
import com.mcc.construction.data.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicySyncRepositoryTest {

    private class TestSync : SyncRepository {
        override val status = MutableStateFlow(
            SyncStatus.Initial
        )

        override fun observeHistory(): Flow<List<OfflineSync>> =
            flowOf(emptyList())

        override suspend fun setOfflineMode(enabled: Boolean) {
            status.value = status.value.copy(
                isOfflineMode = enabled
            )
        }

        override suspend fun syncNow(): SyncResult =
            SyncResult.Success(0)
    }

    private class TestSettings : SystemSettingsRepository {
        override val settings = MutableStateFlow(
            SystemSettings()
        )

        override suspend fun update(settings: SystemSettings) {
            this.settings.value = settings
        }
    }

    @Test
    fun disablingPermissionSwitchesExistingOfflineModeOff() = runTest {
        val delegate = TestSync()
        delegate.setOfflineMode(true)

        val settings = TestSettings()
        val repository = PolicySyncRepository(
            delegate,
            settings,
            backgroundScope
        )

        runCurrent()

        settings.update(
            settings.settings.value.copy(
                allowOfflineMode = false
            )
        )

        runCurrent()

        assertFalse(repository.status.value.isOfflineMode)
        assertFalse(repository.offlineAllowed)
    }

    @Test
    fun prohibitedOfflineModeCannotBeEnabled() = runTest {
        val delegate = TestSync()
        val settings = TestSettings()

        settings.update(
            SystemSettings(allowOfflineMode = false)
        )

        val repository = PolicySyncRepository(
            delegate,
            settings,
            backgroundScope
        )

        var rejected = false

        try {
            repository.setOfflineMode(true)
        } catch (_: UserFacingException) {
            rejected = true
        }

        assertTrue(rejected)
        assertFalse(delegate.status.value.isOfflineMode)
    }

    @Test
    fun prohibitedOfflineMessageIsRejected() = runTest {
        val delegate = TestSync()
        delegate.status.value = delegate.status.value.copy(
            isNetworkAvailable = false
        )

        val settings = TestSettings()
        settings.update(
            SystemSettings(allowOfflineMode = false)
        )

        val repository = PolicySyncRepository(
            delegate,
            settings,
            backgroundScope
        )

        var rejected = false

        try {
            repository.requireMessageQueuePermission()
        } catch (_: UserFacingException) {
            rejected = true
        }

        assertTrue(rejected)
    }

    @Test
    fun onlineMessagesRemainPermittedWhenQueuingIsDisabled() = runTest {
        val settings = TestSettings()
        settings.update(
            SystemSettings(allowOfflineMode = false)
        )

        val repository = PolicySyncRepository(
            TestSync(),
            settings,
            backgroundScope
        )

        repository.requireMessageQueuePermission()
    }
}