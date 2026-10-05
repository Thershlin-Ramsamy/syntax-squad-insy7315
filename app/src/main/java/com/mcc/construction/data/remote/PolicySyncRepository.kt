package com.mcc.construction.data.remote

import com.mcc.construction.data.model.SystemSettings
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.SystemSettingsRepository
import com.mcc.construction.data.repository.UserFacingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PolicySyncRepository(
    private val delegate: SyncRepository,
    private val organisation: SystemSettingsRepository,
    scope: CoroutineScope
) : SyncRepository by delegate {

    val organisationSettings: StateFlow<SystemSettings>
        get() = organisation.settings

    val offlineAllowed: Boolean
        get() = organisation.settings.value.allowOfflineMode

    init {
        scope.launch {
            combine(
                organisation.settings,
                delegate.status
            ) { settings, status ->
                !settings.allowOfflineMode && status.isOfflineMode
            }.collect { mustDisable ->
                if (mustDisable) {
                    try {
                        delegate.setOfflineMode(false)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        /*
                         * Keep observing; a later status or settings
                         * emission can retry the local preference write.
                         */
                    }
                }
            }
        }
    }

    override suspend fun setOfflineMode(enabled: Boolean) {
        if (enabled && !offlineAllowed) {
            throw UserFacingException(
                "Your administrator has disabled offline mode. " +
                        "Connect to the internet to submit changes."
            )
        }

        delegate.setOfflineMode(enabled)
    }

    fun requireMessageQueuePermission() {
        if (!offlineAllowed && !status.value.isOnline) {
            throw UserFacingException(
                "Your administrator has disabled offline messaging. " +
                        "Your draft has been kept. Connect to the internet " +
                        "before sending."
            )
        }
    }
}