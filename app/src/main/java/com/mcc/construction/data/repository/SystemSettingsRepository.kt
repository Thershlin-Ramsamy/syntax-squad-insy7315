package com.mcc.construction.data.repository

import com.mcc.construction.data.model.SystemSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Organisation-wide settings, changed by administrators. Mirrors the class
 * diagram's `Administrator.manageSystemSettings()`.
 */
interface SystemSettingsRepository {

    val settings: StateFlow<SystemSettings>

    /** Saves [settings]; every change is recorded in the audit log. */
    suspend fun update(settings: SystemSettings)
}
