package com.mcc.construction.data.model

/**
 * On-device preferences. Offline mode is not here: it belongs to
 * [SyncStatus], because it changes what the sync indicator shows.
 */
data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
