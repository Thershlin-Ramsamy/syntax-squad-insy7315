package com.mcc.construction.data.repository

import com.mcc.construction.data.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {

    val settings: StateFlow<AppSettings>

    suspend fun setNotificationsEnabled(enabled: Boolean)

    suspend fun setVibrationEnabled(enabled: Boolean)
}
