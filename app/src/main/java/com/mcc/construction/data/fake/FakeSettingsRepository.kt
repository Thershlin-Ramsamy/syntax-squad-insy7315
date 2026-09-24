package com.mcc.construction.data.fake

import com.mcc.construction.data.model.AppSettings
import com.mcc.construction.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [SettingsRepository]. A real one would persist to DataStore. */
class FakeSettingsRepository : SettingsRepository {

    private val _settings = MutableStateFlow(AppSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        _settings.update { it.copy(notificationsEnabled = enabled) }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        _settings.update { it.copy(vibrationEnabled = enabled) }
    }
}
