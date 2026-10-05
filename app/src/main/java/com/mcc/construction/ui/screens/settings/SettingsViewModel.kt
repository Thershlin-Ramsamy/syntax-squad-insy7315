package com.mcc.construction.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.BuildConfig
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SettingsRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.runBackendMutation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.Initial,
    val signedInRole: UserRole? = null,
    val appVersion: String = "",
    val isDemoData: Boolean = false
) {
    val isOfflineMode: Boolean
        get() = syncStatus.isOfflineMode
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    authRepository: AuthRepository,
    appVersion: String,
    isDemoData: Boolean
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        syncRepository.status,
        authRepository.session
    ) { settings, syncStatus, session ->
        SettingsUiState(
            notificationsEnabled = settings.notificationsEnabled,
            vibrationEnabled = settings.vibrationEnabled,
            syncStatus = syncStatus,
            signedInRole = session?.role,
            appVersion = appVersion,
            isDemoData = isDemoData
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(
            notificationsEnabled =
                settingsRepository.settings.value.notificationsEnabled,
            vibrationEnabled =
                settingsRepository.settings.value.vibrationEnabled,
            syncStatus = syncRepository.status.value,
            signedInRole = authRepository.session.value?.role,
            appVersion = appVersion,
            isDemoData = isDemoData
        )
    )

    fun onNotificationsChange(enabled: Boolean) {
        viewModelScope.launch {
            runBackendMutation {
                settingsRepository.setNotificationsEnabled(enabled)
            }
        }
    }

    fun onVibrationChange(enabled: Boolean) {
        viewModelScope.launch {
            runBackendMutation {
                settingsRepository.setVibrationEnabled(enabled)
            }
        }
    }

    fun onOfflineModeChange(enabled: Boolean) {
        viewModelScope.launch {
            runBackendMutation {
                syncRepository.setOfflineMode(enabled)
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    syncRepository = container.syncRepository,
                    authRepository = container.authRepository,
                    appVersion =
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    isDemoData = container.demoAccounts.isNotEmpty()
                )
            }
        }
    }
}