package com.mcc.construction.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.BuildConfig
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.*
import com.mcc.construction.ui.common.BackendFeedback
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.launchSubmission
import kotlinx.coroutines.flow.*

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.Initial,
    val signedInRole: UserRole? = null,
    val appVersion: String = "",
    val isDemoData: Boolean = false
) {
    val isOfflineMode: Boolean get() = syncStatus.isOfflineMode
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    authRepository: AuthRepository,
    appVersion: String,
    isDemoData: Boolean
) : ViewModel() {

    private var isSaving = false

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
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        SettingsUiState(
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
        save {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun onVibrationChange(enabled: Boolean) {
        save {
            settingsRepository.setVibrationEnabled(enabled)
        }
    }

    fun onOfflineModeChange(enabled: Boolean) {
        save {
            syncRepository.setOfflineMode(enabled)
        }
    }

    private fun save(action: suspend () -> Unit) {
        if (isSaving) {
            BackendFeedback.reportFailure(
                "A settings change is still being saved. Try again shortly."
            )
            return
        }

        launchSubmission(
            isBusy = { isSaving },
            setBusy = { isSaving = it },
            failureMessage =
                "The settings change could not be saved. Your previous " +
                        "settings remain selected."
        ) {
            action()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                SettingsViewModel(
                    c.settingsRepository,
                    c.syncRepository,
                    c.authRepository,
                    "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    c.demoAccounts.isNotEmpty()
                )
            }
        }
    }
}