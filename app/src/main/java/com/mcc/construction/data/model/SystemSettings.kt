package com.mcc.construction.data.model

/**
 * Organisation-wide settings an administrator controls. Not an ERD table yet —
 * see "Fields beyond the ERD" in docs/ARCHITECTURE.md.
 */
data class SystemSettings(
    /** Signed-in sessions end after this many idle minutes. */
    val sessionTimeoutMinutes: Int = 30,
    /** Workers must pass the daily safety checklist before starting tasks. */
    val requireDailyChecklist: Boolean = true,
    /** Workers may switch on offline mode and queue changes on the device. */
    val allowOfflineMode: Boolean = true,
    /** Managers are notified when a material drops below its minimum level. */
    val lowStockAlerts: Boolean = true,
    /** How often devices upload queued changes in the background, in minutes. */
    val syncIntervalMinutes: Int = 15
) {
    companion object {
        val SESSION_TIMEOUT_OPTIONS = listOf(15, 30, 60, 120)
        val SYNC_INTERVAL_OPTIONS = listOf(5, 15, 30, 60)
    }
}
