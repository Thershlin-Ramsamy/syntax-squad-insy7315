package com.mcc.construction.data.model

/**
 * Organisation settings stored in public.system_settings.
 */
data class SystemSettings(
    /** Idle timeout applied by the Android application. */
    val sessionTimeoutMinutes: Int = 30,

    /** Server-enforced daily safety requirement before starting tasks. */
    val requireDailyChecklist: Boolean = true,

    /** Allows offline mode and new offline message queuing. */
    val allowOfflineMode: Boolean = true,

    /** Enables server-generated low-stock alerts. */
    val lowStockAlerts: Boolean = true,

    /** Interval between automatic foreground message sync attempts. */
    val syncIntervalMinutes: Int = 15
) {
    companion object {
        val SESSION_TIMEOUT_OPTIONS = listOf(15, 30, 60, 120)
        val SYNC_INTERVAL_OPTIONS = listOf(5, 15, 30, 60)
    }
}