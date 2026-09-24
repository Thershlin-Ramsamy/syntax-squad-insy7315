package com.mcc.construction.data.fake

import com.mcc.construction.data.model.SystemSettings
import com.mcc.construction.data.repository.SystemSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [SystemSettingsRepository] that records each change in the audit log. */
class FakeSystemSettingsRepository(
    private val auditLog: FakeAuditLogRepository,
    private val currentUserId: () -> Int?
) : SystemSettingsRepository {

    private val _settings = MutableStateFlow(SystemSettings())
    override val settings: StateFlow<SystemSettings> = _settings.asStateFlow()

    override suspend fun update(settings: SystemSettings) {
        val before = _settings.value
        if (before == settings) return
        _settings.value = settings
        val actor = currentUserId() ?: return
        describeChanges(before, settings).forEach { auditLog.record(actor, it) }
    }

    private fun describeChanges(before: SystemSettings, after: SystemSettings): List<String> = buildList {
        if (before.sessionTimeoutMinutes != after.sessionTimeoutMinutes) {
            add("Updated system setting: session timeout ${after.sessionTimeoutMinutes} minutes")
        }
        if (before.requireDailyChecklist != after.requireDailyChecklist) {
            add("Updated system setting: daily safety checklist ${onOff(after.requireDailyChecklist)}")
        }
        if (before.allowOfflineMode != after.allowOfflineMode) {
            add("Updated system setting: offline mode ${onOff(after.allowOfflineMode)}")
        }
        if (before.lowStockAlerts != after.lowStockAlerts) {
            add("Updated system setting: low stock alerts ${onOff(after.lowStockAlerts)}")
        }
        if (before.syncIntervalMinutes != after.syncIntervalMinutes) {
            add("Updated system setting: background sync every ${after.syncIntervalMinutes} minutes")
        }
    }

    private fun onOff(value: Boolean) = if (value) "required" else "off"
}
