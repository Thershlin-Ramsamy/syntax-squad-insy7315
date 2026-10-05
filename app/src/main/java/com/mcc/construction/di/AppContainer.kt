package com.mcc.construction.di

import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.AuditLogRepository
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.DailyReportRepository
import com.mcc.construction.data.repository.DocumentRepository
import com.mcc.construction.data.repository.InventoryRepository
import com.mcc.construction.data.repository.MessageRepository
import com.mcc.construction.data.repository.NotificationRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.SafetyRepository
import com.mcc.construction.data.repository.SettingsRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.SystemSettingsRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import java.time.Clock

interface AppContainer {
    fun recordUserInteraction() {}

    val authRepository: AuthRepository
    val userRepository: UserRepository
    val projectRepository: ProjectRepository
    val taskRepository: TaskRepository
    val attendanceRepository: AttendanceRepository
    val dailyReportRepository: DailyReportRepository
    val inventoryRepository: InventoryRepository
    val documentRepository: DocumentRepository
    val safetyRepository: SafetyRepository
    val notificationRepository: NotificationRepository
    val messageRepository: MessageRepository
    val auditLogRepository: AuditLogRepository
    val syncRepository: SyncRepository
    val settingsRepository: SettingsRepository
    val systemSettingsRepository: SystemSettingsRepository
    val clock: Clock
    val demoAccounts: List<DemoAccount>
}

data class DemoAccount(
    val role: UserRole,
    val email: String
)